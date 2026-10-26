package com.mallmini.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mallmini.address.entity.UserAddress;
import com.mallmini.address.service.AddressService;
import com.mallmini.admin.service.AdminOperationLogger;
import com.mallmini.cart.entity.CartItem;
import com.mallmini.cart.mapper.CartItemMapper;
import com.mallmini.category.service.CategoryService;
import com.mallmini.common.api.PageResult;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.common.util.MaskUtils;
import com.mallmini.config.AppProperties;
import com.mallmini.order.dto.CheckoutPreviewRequest;
import com.mallmini.order.dto.CreateOrderRequest;
import com.mallmini.order.entity.MallOrder;
import com.mallmini.order.entity.MallOrderItem;
import com.mallmini.order.entity.OrderStatusLog;
import com.mallmini.order.enums.OrderOperatorType;
import com.mallmini.order.enums.OrderSource;
import com.mallmini.order.enums.OrderStatus;
import com.mallmini.order.enums.PaymentStatus;
import com.mallmini.order.mapper.MallOrderItemMapper;
import com.mallmini.order.mapper.MallOrderMapper;
import com.mallmini.order.mapper.OrderStatusLogMapper;
import com.mallmini.order.vo.AddressSnapshotVO;
import com.mallmini.order.vo.AdminOrderSummaryVO;
import com.mallmini.order.vo.CheckoutVO;
import com.mallmini.order.vo.OrderDetailVO;
import com.mallmini.order.vo.OrderItemVO;
import com.mallmini.order.vo.OrderStatusLogVO;
import com.mallmini.order.vo.OrderSummaryVO;
import com.mallmini.product.entity.Product;
import com.mallmini.product.entity.ProductSku;
import com.mallmini.product.enums.ProductStatus;
import com.mallmini.product.enums.SkuStatus;
import com.mallmini.product.mapper.ProductMapper;
import com.mallmini.product.mapper.ProductSkuMapper;
import com.mallmini.product.service.SpecJsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final int MAX_QUANTITY = 99;

    private final MallOrderMapper orderMapper;
    private final MallOrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductMapper productMapper;
    private final CategoryService categoryService;
    private final AddressService addressService;
    private final CartItemMapper cartItemMapper;
    private final SpecJsonParser specJsonParser;
    private final AppProperties properties;
    private final AdminOperationLogger operationLogger;

    public CheckoutVO preview(Long userId, CheckoutPreviewRequest request) {
        PreparedCheckout prepared = prepare(
                userId,
                request.addressId(),
                request.source(),
                request.cartItemIds(),
                request.skuId(),
                request.quantity());
        return toCheckoutVO(prepared);
    }

    @Transactional
    public OrderDetailVO create(Long userId, CreateOrderRequest request) {
        String requestNo = request.clientRequestNo().trim();
        MallOrder existing = orderMapper.selectOne(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getUserId, userId)
                .eq(MallOrder::getClientRequestNo, requestNo));
        if (existing != null) {
            return toDetail(existing);
        }

        PreparedCheckout prepared = prepare(
                userId,
                request.addressId(),
                request.source(),
                request.cartItemIds(),
                request.skuId(),
                request.quantity());

        UserAddress address = prepared.address();
        MallOrder order = new MallOrder();
        order.setOrderNo("MM" + IdWorker.getIdStr());
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING_PAYMENT.name());
        order.setPaymentStatus(PaymentStatus.UNPAID.name());
        order.setGoodsAmount(prepared.amounts().goods());
        order.setDiscountAmount(prepared.amounts().discount());
        order.setShippingFee(prepared.amounts().shipping());
        order.setPayableAmount(prepared.amounts().payable());
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getPhone());
        order.setReceiverProvince(address.getProvince());
        order.setReceiverCity(address.getCity());
        order.setReceiverDistrict(address.getDistrict());
        order.setReceiverDetail(address.getDetail());
        order.setClientRequestNo(requestNo);
        order.setRemark(request.remark() == null ? "" : request.remark().trim());
        orderMapper.insert(order);

        for (Line line : prepared.lines()) {
            int affected = productSkuMapper.deductStock(line.skuId(), line.quantity());
            if (affected != 1) {
                throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT,
                        "库存不足：" + line.productName() + " / " + line.skuSpec());
            }
        }

        for (Line line : prepared.lines()) {
            MallOrderItem item = new MallOrderItem();
            item.setOrderId(order.getId());
            item.setOrderNo(order.getOrderNo());
            item.setProductId(line.productId());
            item.setSkuId(line.skuId());
            item.setProductName(line.productName());
            item.setSkuSpec(line.skuSpec());
            item.setProductImage(line.productImage());
            item.setUnitPrice(line.unitPrice());
            item.setQuantity(line.quantity());
            item.setSubtotal(line.subtotal());
            orderItemMapper.insert(item);
        }

        writeLog(order, null, OrderStatus.PENDING_PAYMENT, OrderOperatorType.USER, userId, "创建订单");

        if (OrderSource.CART == OrderSource.from(request.source())
                && prepared.cartItemIds() != null && !prepared.cartItemIds().isEmpty()) {
            cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getUserId, userId)
                    .in(CartItem::getId, prepared.cartItemIds()));
        }

        MallOrder saved = orderMapper.selectById(order.getId());
        return toDetail(saved == null ? order : saved);
    }

    public PageResult<OrderSummaryVO> list(Long userId, String statusValue, long page, long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<MallOrder> wrapper = new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getUserId, userId)
                .orderByDesc(MallOrder::getCreatedAt)
                .orderByDesc(MallOrder::getId);
        if (statusValue != null && !statusValue.isBlank()) {
            wrapper.eq(MallOrder::getStatus, OrderStatus.from(statusValue).name());
        }

        Page<MallOrder> orderPage = orderMapper.selectPage(new Page<>(safePage, safePageSize), wrapper);
        List<MallOrder> orders = orderPage.getRecords();
        if (orders.isEmpty()) {
            return PageResult.of(List.of(), safePage, safePageSize, orderPage.getTotal());
        }

        List<Long> orderIds = orders.stream().map(MallOrder::getId).toList();
        Map<Long, List<MallOrderItem>> itemsByOrder = orderItemMapper.selectList(
                        new LambdaQueryWrapper<MallOrderItem>()
                                .in(MallOrderItem::getOrderId, orderIds)
                                .orderByAsc(MallOrderItem::getId))
                .stream()
                .collect(Collectors.groupingBy(MallOrderItem::getOrderId, LinkedHashMap::new, Collectors.toList()));

        List<OrderSummaryVO> items = orders.stream().map(order -> {
            List<MallOrderItem> orderItems = itemsByOrder.getOrDefault(order.getId(), List.of());
            int totalQuantity = orderItems.stream().mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity()).sum();
            MallOrderItem first = orderItems.isEmpty() ? null : orderItems.get(0);
            return new OrderSummaryVO(
                    order.getOrderNo(),
                    order.getStatus(),
                    order.getPaymentStatus(),
                    order.getPayableAmount(),
                    totalQuantity,
                    first == null ? "" : first.getProductName(),
                    first == null ? "" : first.getProductImage(),
                    order.getCreatedAt());
        }).toList();

        return PageResult.of(items, safePage, safePageSize, orderPage.getTotal());
    }

    public OrderDetailVO detail(Long userId, String orderNo) {
        MallOrder order = requireOwned(userId, orderNo);
        return toDetail(order);
    }

    public PageResult<AdminOrderSummaryVO> adminList(String statusValue,
                                                     String orderNo,
                                                     Long userId,
                                                     long page,
                                                     long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<MallOrder> wrapper = new LambdaQueryWrapper<MallOrder>()
                .orderByDesc(MallOrder::getCreatedAt)
                .orderByDesc(MallOrder::getId);
        if (statusValue != null && !statusValue.isBlank()) {
            wrapper.eq(MallOrder::getStatus, OrderStatus.from(statusValue).name());
        }
        if (orderNo != null && !orderNo.isBlank()) {
            wrapper.like(MallOrder::getOrderNo, orderNo.trim());
        }
        if (userId != null) {
            wrapper.eq(MallOrder::getUserId, userId);
        }

        Page<MallOrder> orderPage = orderMapper.selectPage(new Page<>(safePage, safePageSize), wrapper);
        List<MallOrder> orders = orderPage.getRecords();
        if (orders.isEmpty()) {
            return PageResult.of(List.of(), safePage, safePageSize, orderPage.getTotal());
        }

        Map<Long, List<MallOrderItem>> itemsByOrder = orderItemMapper.selectList(
                        new LambdaQueryWrapper<MallOrderItem>()
                                .in(MallOrderItem::getOrderId, orders.stream().map(MallOrder::getId).toList()))
                .stream()
                .collect(Collectors.groupingBy(MallOrderItem::getOrderId));

        List<AdminOrderSummaryVO> items = orders.stream().map(order -> {
            int totalQuantity = itemsByOrder.getOrDefault(order.getId(), List.of()).stream()
                    .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                    .sum();
            return new AdminOrderSummaryVO(
                    order.getOrderNo(),
                    order.getUserId(),
                    order.getStatus(),
                    order.getPaymentStatus(),
                    order.getPayableAmount(),
                    totalQuantity,
                    order.getReceiverName(),
                    MaskUtils.maskPhone(order.getReceiverPhone()),
                    order.getCreatedAt());
        }).toList();
        return PageResult.of(items, safePage, safePageSize, orderPage.getTotal());
    }

    public OrderDetailVO adminDetail(String orderNo) {
        return toDetail(adminRequire(orderNo));
    }

    @Transactional
    public OrderDetailVO ship(String orderNo, String logisticsCompany, String logisticsNo, Long adminId) {
        MallOrder order = adminRequire(orderNo);
        if (OrderStatus.PENDING_RECEIPT.name().equals(order.getStatus())) {
            return toDetail(order);
        }
        if (!OrderStatus.PENDING_SHIPMENT.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "当前订单状态不可发货");
        }

        int updated = orderMapper.transitionToShipped(order.getId(),
                logisticsCompany.trim(), logisticsNo.trim());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "订单状态已变化，请刷新后重试");
        }
        writeLog(order, OrderStatus.PENDING_SHIPMENT, OrderStatus.PENDING_RECEIPT,
                OrderOperatorType.ADMIN, adminId, "发货：" + logisticsCompany.trim() + " " + logisticsNo.trim());
        operationLogger.log(adminId, "ORDER_SHIP", "ORDER", orderNo,
                logisticsCompany.trim() + " " + logisticsNo.trim());
        return toDetail(requireById(order.getId()));
    }

    @Transactional
    public OrderDetailVO adminCancel(String orderNo, String reason, Long adminId) {
        MallOrder order = adminRequire(orderNo);
        if (OrderStatus.CANCELLED.name().equals(order.getStatus())) {
            return toDetail(order);
        }
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "当前订单状态不可取消");
        }

        String cancelReason = reason == null || reason.isBlank() ? "管理员取消" : reason.trim();
        int updated = orderMapper.transitionToCancelled(
                order.getId(), OrderStatus.PENDING_PAYMENT.name(), cancelReason);
        if (updated == 0) {
            MallOrder latest = orderMapper.selectById(order.getId());
            if (latest != null && OrderStatus.CANCELLED.name().equals(latest.getStatus())) {
                return toDetail(latest);
            }
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "订单状态已变化，请刷新后重试");
        }

        restoreStock(order.getId());
        writeLog(order, OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED,
                OrderOperatorType.ADMIN, adminId, cancelReason);
        operationLogger.log(adminId, "ORDER_CANCEL", "ORDER", orderNo, cancelReason);
        return toDetail(requireById(order.getId()));
    }

    @Transactional
    public OrderDetailVO cancel(Long userId, String orderNo, String reason) {
        MallOrder order = requireOwned(userId, orderNo);
        if (OrderStatus.CANCELLED.name().equals(order.getStatus())) {
            return toDetail(order);
        }
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "当前订单状态不可取消");
        }

        int cancelMinutes = properties.getOrder().getCancelMinutes();
        if (cancelMinutes > 0 && order.getCreatedAt() != null
                && order.getCreatedAt().plusMinutes(cancelMinutes).isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "已超过取消时限");
        }

        String cancelReason = reason == null || reason.isBlank() ? "用户取消" : reason.trim();
        int updated = orderMapper.transitionToCancelled(
                order.getId(), OrderStatus.PENDING_PAYMENT.name(), cancelReason);
        if (updated == 0) {
            MallOrder latest = orderMapper.selectById(order.getId());
            if (latest != null && OrderStatus.CANCELLED.name().equals(latest.getStatus())) {
                return toDetail(latest);
            }
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "订单状态已变化，请刷新后重试");
        }

        restoreStock(order.getId());
        writeLog(order, OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED,
                OrderOperatorType.USER, userId, cancelReason);
        return toDetail(requireById(order.getId()));
    }

    @Transactional
    public OrderDetailVO confirmReceipt(Long userId, String orderNo) {
        MallOrder order = requireOwned(userId, orderNo);
        if (OrderStatus.COMPLETED.name().equals(order.getStatus())) {
            return toDetail(order);
        }
        if (!OrderStatus.PENDING_RECEIPT.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "当前订单状态不可确认收货");
        }

        int updated = orderMapper.transitionToCompleted(order.getId());
        if (updated == 0) {
            MallOrder latest = orderMapper.selectById(order.getId());
            if (latest != null && OrderStatus.COMPLETED.name().equals(latest.getStatus())) {
                return toDetail(latest);
            }
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "订单状态已变化，请刷新后重试");
        }

        writeLog(order, OrderStatus.PENDING_RECEIPT, OrderStatus.COMPLETED,
                OrderOperatorType.USER, userId, "确认收货");
        return toDetail(requireById(order.getId()));
    }

    @Transactional
    public void systemCancel(Long orderId) {
        MallOrder order = orderMapper.selectById(orderId);
        if (order == null || !OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())) {
            return;
        }
        int updated = orderMapper.transitionToCancelled(
                orderId, OrderStatus.PENDING_PAYMENT.name(), "超时未支付");
        if (updated == 0) {
            return;
        }
        restoreStock(orderId);
        writeLog(order, OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED,
                OrderOperatorType.SYSTEM, null, "超时未支付");
    }

    private PreparedCheckout prepare(Long userId,
                                     Long addressId,
                                     String sourceValue,
                                     List<Long> cartItemIds,
                                     Long skuId,
                                     Integer quantity) {
        UserAddress address = addressService.requireOwned(userId, addressId);
        OrderSource source = OrderSource.from(sourceValue);

        List<Long> normalizedCartItemIds = null;
        List<Line> lines;
        if (source == OrderSource.CART) {
            if (cartItemIds == null || cartItemIds.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请选择结算商品");
            }
            normalizedCartItemIds = cartItemIds.stream().filter(Objects::nonNull).distinct().toList();
            if (normalizedCartItemIds.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请选择结算商品");
            }
            List<CartItem> cartItems = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getUserId, userId)
                    .in(CartItem::getId, normalizedCartItemIds));
            if (cartItems.size() != normalizedCartItemIds.size()) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "购物车商品不存在或已变更");
            }
            lines = cartItems.stream()
                    .map(item -> buildLine(item.getSkuId(), item.getQuantity()))
                    .sorted(Comparator.comparing(Line::skuId))
                    .toList();
        } else {
            if (skuId == null || quantity == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请选择商品与数量");
            }
            if (quantity > MAX_QUANTITY) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "单项数量不能超过 99");
            }
            lines = List.of(buildLine(skuId, quantity));
        }

        Amounts amounts = calculateAmounts(lines);
        return new PreparedCheckout(normalizedCartItemIds, address, lines, amounts);
    }

    private Line buildLine(Long skuId, Integer quantity) {
        if (skuId == null || quantity == null || quantity < 1 || quantity > MAX_QUANTITY) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "商品数量不合法");
        }
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null || !SkuStatus.ENABLED.name().equals(sku.getStatus())) {
            throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE, "商品规格不存在或已停用");
        }
        Product product = productMapper.selectById(sku.getProductId());
        if (product == null
                || !ProductStatus.ON_SALE.name().equals(product.getStatus())
                || !categoryService.isEnabled(product.getCategoryId())) {
            throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE, "商品已下架或不可购买");
        }
        if (sku.getStock() == null || sku.getStock() < quantity) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT,
                    "库存不足：" + product.getName());
        }
        BigDecimal price = sku.getPrice() == null ? ZERO : sku.getPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        String specText = String.join(" / ", specJsonParser.parse(sku.getSpecJson()).values());
        return new Line(
                product.getId(),
                sku.getId(),
                product.getName(),
                specText.isBlank() ? "默认规格" : specText,
                product.getMainImage(),
                price,
                quantity,
                subtotal);
    }

    private Amounts calculateAmounts(List<Line> lines) {
        BigDecimal goods = lines.stream()
                .map(Line::subtotal)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal threshold = properties.getShipping().getFreeThreshold();
        BigDecimal fee = properties.getShipping().getFee();
        BigDecimal shipping;
        if (threshold == null || threshold.signum() <= 0 || goods.compareTo(threshold) >= 0) {
            shipping = ZERO;
        } else {
            shipping = fee == null ? ZERO : fee.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal discount = ZERO;
        BigDecimal payable = goods.add(shipping).subtract(discount).setScale(2, RoundingMode.HALF_UP);
        return new Amounts(goods, discount, shipping, payable);
    }

    private CheckoutVO toCheckoutVO(PreparedCheckout prepared) {
        List<OrderItemVO> items = prepared.lines().stream()
                .map(line -> new OrderItemVO(
                        line.productId(),
                        line.skuId(),
                        line.productName(),
                        line.skuSpec(),
                        line.productImage(),
                        line.unitPrice(),
                        line.quantity(),
                        line.subtotal()))
                .toList();
        return new CheckoutVO(
                toAddressSnapshot(prepared.address()),
                items,
                prepared.amounts().goods(),
                prepared.amounts().discount(),
                prepared.amounts().shipping(),
                prepared.amounts().payable(),
                properties.getShipping().getFreeThreshold());
    }

    private OrderDetailVO toDetail(MallOrder order) {
        List<MallOrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<MallOrderItem>()
                .eq(MallOrderItem::getOrderId, order.getId())
                .orderByAsc(MallOrderItem::getId));
        List<OrderItemVO> items = orderItems.stream()
                .map(item -> new OrderItemVO(
                        item.getProductId(),
                        item.getSkuId(),
                        item.getProductName(),
                        item.getSkuSpec(),
                        item.getProductImage(),
                        item.getUnitPrice(),
                        item.getQuantity() == null ? 0 : item.getQuantity(),
                        item.getSubtotal()))
                .toList();
        List<OrderStatusLogVO> logs = statusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                        .eq(OrderStatusLog::getOrderId, order.getId())
                        .orderByAsc(OrderStatusLog::getCreatedAt)
                        .orderByAsc(OrderStatusLog::getId))
                .stream()
                .map(log -> new OrderStatusLogVO(
                        log.getFromStatus(),
                        log.getToStatus(),
                        log.getOperatorType(),
                        log.getRemark(),
                        log.getCreatedAt()))
                .toList();

        return new OrderDetailVO(
                order.getOrderNo(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getGoodsAmount(),
                order.getDiscountAmount(),
                order.getShippingFee(),
                order.getPayableAmount(),
                new AddressSnapshotVO(
                        order.getReceiverName(),
                        order.getReceiverPhone(),
                        order.getReceiverProvince(),
                        order.getReceiverCity(),
                        order.getReceiverDistrict(),
                        order.getReceiverDetail()),
                items,
                order.getRemark(),
                order.getLogisticsCompany(),
                order.getLogisticsNo(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getShippedAt(),
                order.getCompletedAt(),
                order.getCancelledAt(),
                order.getCancelReason(),
                logs);
    }

    private AddressSnapshotVO toAddressSnapshot(UserAddress address) {
        return new AddressSnapshotVO(
                address.getReceiverName(),
                address.getPhone(),
                address.getProvince(),
                address.getCity(),
                address.getDistrict(),
                address.getDetail());
    }

    private MallOrder requireOwned(Long userId, String orderNo) {
        MallOrder order = orderMapper.selectOne(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getOrderNo, orderNo)
                .eq(MallOrder::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private MallOrder adminRequire(String orderNo) {
        MallOrder order = orderMapper.selectOne(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private MallOrder requireById(Long orderId) {
        MallOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private void restoreStock(Long orderId) {
        List<MallOrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<MallOrderItem>()
                .eq(MallOrderItem::getOrderId, orderId));
        for (MallOrderItem item : items) {
            int affected = productSkuMapper.restoreStock(
                    item.getSkuId(), item.getQuantity() == null ? 0 : item.getQuantity());
            if (affected != 1) {
                log.warn("failed to restore stock for sku {} order {}", item.getSkuId(), orderId);
            }
        }
    }

    private void writeLog(MallOrder order,
                          OrderStatus from,
                          OrderStatus to,
                          OrderOperatorType operatorType,
                          Long operatorId,
                          String remark) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(order.getId());
        log.setFromStatus(from == null ? null : from.name());
        log.setToStatus(to.name());
        log.setOperatorType(operatorType.name());
        log.setOperatorId(operatorId);
        log.setRemark(remark == null ? "" : remark);
        statusLogMapper.insert(log);
    }

    private record Line(
            Long productId,
            Long skuId,
            String productName,
            String skuSpec,
            String productImage,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal
    ) {
    }

    private record Amounts(
            BigDecimal goods,
            BigDecimal discount,
            BigDecimal shipping,
            BigDecimal payable
    ) {
    }

    private record PreparedCheckout(
            List<Long> cartItemIds,
            UserAddress address,
            List<Line> lines,
            Amounts amounts
    ) {
    }
}
