package com.mallmini.aftersale.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mallmini.admin.service.AdminOperationLogger;
import com.mallmini.aftersale.dto.AdminRefundRequest;
import com.mallmini.aftersale.dto.CreateAfterSaleRequest;
import com.mallmini.aftersale.entity.AfterSaleItem;
import com.mallmini.aftersale.entity.AfterSaleLog;
import com.mallmini.aftersale.entity.AfterSaleOrder;
import com.mallmini.aftersale.entity.RefundRecord;
import com.mallmini.aftersale.enums.AfterSaleOperatorType;
import com.mallmini.aftersale.enums.AfterSaleStatus;
import com.mallmini.aftersale.enums.AfterSaleType;
import com.mallmini.aftersale.enums.RefundChannel;
import com.mallmini.aftersale.enums.RefundStatus;
import com.mallmini.aftersale.gateway.RefundGateway;
import com.mallmini.aftersale.gateway.RefundRequest;
import com.mallmini.aftersale.gateway.RefundResult;
import com.mallmini.aftersale.mapper.AfterSaleItemMapper;
import com.mallmini.aftersale.mapper.AfterSaleLogMapper;
import com.mallmini.aftersale.mapper.AfterSaleOrderMapper;
import com.mallmini.aftersale.mapper.RefundRecordMapper;
import com.mallmini.aftersale.vo.AfterSaleDetailVO;
import com.mallmini.aftersale.vo.AfterSaleItemVO;
import com.mallmini.aftersale.vo.AfterSaleLogVO;
import com.mallmini.aftersale.vo.AfterSaleSummaryVO;
import com.mallmini.aftersale.vo.RefundInfoVO;
import com.mallmini.common.api.PageResult;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.inventory.enums.StockChangeType;
import com.mallmini.inventory.service.StockService;
import com.mallmini.member.service.PointsService;
import com.mallmini.notification.service.NotificationService;
import com.mallmini.order.entity.MallOrder;
import com.mallmini.order.entity.MallOrderItem;
import com.mallmini.order.enums.OrderStatus;
import com.mallmini.order.enums.PaymentStatus;
import com.mallmini.order.mapper.MallOrderItemMapper;
import com.mallmini.order.mapper.MallOrderMapper;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AfterSaleService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final AfterSaleOrderMapper afterSaleOrderMapper;
    private final AfterSaleItemMapper afterSaleItemMapper;
    private final AfterSaleLogMapper afterSaleLogMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final MallOrderMapper orderMapper;
    private final MallOrderItemMapper orderItemMapper;
    private final StockService stockService;
    private final NotificationService notificationService;
    private final PointsService pointsService;
    private final AdminOperationLogger operationLogger;
    private final Optional<RefundGateway> refundGateway;

    @Transactional
    public AfterSaleDetailVO create(Long userId, String orderNo, CreateAfterSaleRequest request) {
        MallOrder order = orderMapper.selectOne(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getOrderNo, orderNo)
                .eq(MallOrder::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        if (PaymentStatus.REFUNDED.name().equals(order.getPaymentStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "订单已退款，不能重复申请售后");
        }
        if (!PaymentStatus.PAID.name().equals(order.getPaymentStatus())
                && !PaymentStatus.PARTIALLY_REFUNDED.name().equals(order.getPaymentStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "订单未支付，不能申请售后");
        }
        if (!OrderStatus.PENDING_SHIPMENT.name().equals(order.getStatus())
                && !OrderStatus.PENDING_RECEIPT.name().equals(order.getStatus())
                && !OrderStatus.COMPLETED.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "当前订单状态不能申请售后");
        }

        AfterSaleOrder existing = latestForOrder(order.getId());
        if (existing != null) {
            if (AfterSaleStatus.REFUNDED.name().equals(existing.getStatus())) {
                throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "该订单已退款，不能重复申请");
            }
            if (AfterSaleStatus.from(existing.getStatus()).isActive()) {
                throw new BusinessException(ErrorCode.CONFLICT, "该订单已有进行中的售后");
            }
        }

        AfterSaleType type = AfterSaleType.from(request.type());
        if (type == AfterSaleType.RETURN_REFUND
                && OrderStatus.PENDING_SHIPMENT.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "未发货订单只能申请仅退款");
        }

        List<MallOrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<MallOrderItem>()
                .eq(MallOrderItem::getOrderId, order.getId()));
        if (orderItems.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "订单明细不存在");
        }
        Map<Long, MallOrderItem> itemMap = orderItems.stream()
                .collect(Collectors.toMap(MallOrderItem::getId, item -> item));

        Map<Long, Integer> requested = new LinkedHashMap<>();
        for (CreateAfterSaleRequest.ItemRequest item : request.items()) {
            if (item == null || item.orderItemId() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "售后商品参数不合法");
            }
            if (itemMap.get(item.orderItemId()) == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "售后商品不属于该订单");
            }
            if (requested.putIfAbsent(item.orderItemId(), item.quantity()) != null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "售后商品重复");
            }
        }

        List<AfterSaleItem> items = new ArrayList<>();
        BigDecimal goodsRefund = ZERO;
        boolean fullOrder = true;
        for (MallOrderItem orderItem : orderItems) {
            int bought = orderItem.getQuantity() == null ? 0 : orderItem.getQuantity();
            int quantity = requested.getOrDefault(orderItem.getId(), 0);
            if (quantity < 0 || quantity > bought) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "售后数量不能超过购买数量");
            }
            if (quantity != bought) {
                fullOrder = false;
            }
            if (quantity == 0) {
                continue;
            }
            BigDecimal unitPrice = orderItem.getUnitPrice() == null ? ZERO : orderItem.getUnitPrice();
            BigDecimal itemRefund = unitPrice.multiply(BigDecimal.valueOf(quantity))
                    .setScale(2, RoundingMode.HALF_UP);
            goodsRefund = goodsRefund.add(itemRefund);

            AfterSaleItem item = new AfterSaleItem();
            item.setOrderItemId(orderItem.getId());
            item.setProductId(orderItem.getProductId());
            item.setSkuId(orderItem.getSkuId());
            item.setProductName(orderItem.getProductName());
            item.setSkuSpec(orderItem.getSkuSpec());
            item.setProductImage(orderItem.getProductImage());
            item.setUnitPrice(unitPrice);
            item.setQuantity(quantity);
            item.setRefundAmount(itemRefund);
            items.add(item);
        }
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请选择售后商品");
        }

        BigDecimal refundAmount = fullOrder
                ? order.getPayableAmount().setScale(2, RoundingMode.HALF_UP)
                : goodsRefund.setScale(2, RoundingMode.HALF_UP);
        if (refundAmount.compareTo(order.getPayableAmount()) > 0) {
            refundAmount = order.getPayableAmount().setScale(2, RoundingMode.HALF_UP);
        }

        AfterSaleOrder afterSale = new AfterSaleOrder();
        afterSale.setAfterSaleNo("AS" + IdWorker.getIdStr());
        afterSale.setOrderId(order.getId());
        afterSale.setOrderNo(order.getOrderNo());
        afterSale.setUserId(userId);
        afterSale.setType(type.name());
        afterSale.setStatus(AfterSaleStatus.APPLIED.name());
        afterSale.setReason(request.reason().trim());
        afterSale.setDescription(request.description() == null ? "" : request.description().trim());
        afterSale.setRefundAmount(refundAmount);
        afterSaleOrderMapper.insert(afterSale);

        for (AfterSaleItem item : items) {
            item.setAfterSaleId(afterSale.getId());
            afterSaleItemMapper.insert(item);
        }

        writeLog(afterSale.getId(), null, AfterSaleStatus.APPLIED,
                AfterSaleOperatorType.USER, userId, "用户申请售后");
        notificationService.notifyAdmins(
                "AFTER_SALE_APPLIED",
                "新的售后申请",
                "订单 " + order.getOrderNo() + " 提交了" + typeText(type) + "，退款金额 "
                        + refundAmount.toPlainString() + " 元。",
                "AFTER_SALE",
                afterSale.getAfterSaleNo(),
                Map.of("afterSaleNo", afterSale.getAfterSaleNo(), "orderNo", order.getOrderNo()));

        return detailForUser(userId, afterSale.getAfterSaleNo());
    }

    public PageResult<AfterSaleSummaryVO> listForUser(Long userId,
                                                      String statusValue,
                                                      long page,
                                                      long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<AfterSaleOrder> wrapper = new LambdaQueryWrapper<AfterSaleOrder>()
                .eq(AfterSaleOrder::getUserId, userId)
                .orderByDesc(AfterSaleOrder::getCreatedAt)
                .orderByDesc(AfterSaleOrder::getId);
        if (statusValue != null && !statusValue.isBlank()) {
            wrapper.eq(AfterSaleOrder::getStatus, AfterSaleStatus.from(statusValue).name());
        }
        Page<AfterSaleOrder> result = afterSaleOrderMapper.selectPage(
                new Page<>(safePage, safePageSize), wrapper);
        return toSummaryPage(result, safePage, safePageSize);
    }

    public PageResult<AfterSaleSummaryVO> adminList(String statusValue,
                                                    String afterSaleNo,
                                                    String orderNo,
                                                    long page,
                                                    long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<AfterSaleOrder> wrapper = new LambdaQueryWrapper<AfterSaleOrder>()
                .orderByDesc(AfterSaleOrder::getCreatedAt)
                .orderByDesc(AfterSaleOrder::getId);
        if (statusValue != null && !statusValue.isBlank()) {
            wrapper.eq(AfterSaleOrder::getStatus, AfterSaleStatus.from(statusValue).name());
        }
        if (afterSaleNo != null && !afterSaleNo.isBlank()) {
            wrapper.like(AfterSaleOrder::getAfterSaleNo, afterSaleNo.trim());
        }
        if (orderNo != null && !orderNo.isBlank()) {
            wrapper.like(AfterSaleOrder::getOrderNo, orderNo.trim());
        }
        Page<AfterSaleOrder> result = afterSaleOrderMapper.selectPage(
                new Page<>(safePage, safePageSize), wrapper);
        return toSummaryPage(result, safePage, safePageSize);
    }

    public AfterSaleDetailVO detailForUser(Long userId, String afterSaleNo) {
        AfterSaleOrder afterSale = requireOwned(userId, afterSaleNo);
        return toDetail(afterSale);
    }

    public AfterSaleDetailVO adminDetail(String afterSaleNo) {
        return toDetail(requireByNo(afterSaleNo));
    }

    @Transactional
    public AfterSaleDetailVO cancel(Long userId, String afterSaleNo) {
        AfterSaleOrder afterSale = requireOwned(userId, afterSaleNo);
        if (AfterSaleStatus.CANCELLED.name().equals(afterSale.getStatus())) {
            return toDetail(afterSale);
        }
        int updated = afterSaleOrderMapper.cancel(afterSale.getId());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "当前售后状态不能取消");
        }
        writeLog(afterSale.getId(), AfterSaleStatus.from(afterSale.getStatus()),
                AfterSaleStatus.CANCELLED, AfterSaleOperatorType.USER, userId, "用户取消售后");
        return toDetail(requireById(afterSale.getId()));
    }

    @Transactional
    public AfterSaleDetailVO approve(Long adminId, String afterSaleNo) {
        AfterSaleOrder afterSale = requireByNo(afterSaleNo);
        if (AfterSaleStatus.APPROVED.name().equals(afterSale.getStatus())
                || AfterSaleStatus.WAIT_RETURN.name().equals(afterSale.getStatus())
                || AfterSaleStatus.RETURNED.name().equals(afterSale.getStatus())
                || AfterSaleStatus.RETURN_RECEIVED.name().equals(afterSale.getStatus())
                || AfterSaleStatus.REFUNDED.name().equals(afterSale.getStatus())) {
            return toDetail(afterSale);
        }
        if (!AfterSaleStatus.APPLIED.name().equals(afterSale.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "当前售后状态不能审核通过");
        }
        AfterSaleStatus target = AfterSaleType.RETURN_REFUND.name().equals(afterSale.getType())
                ? AfterSaleStatus.WAIT_RETURN : AfterSaleStatus.APPROVED;
        int updated = afterSaleOrderMapper.approve(afterSale.getId(), target.name(), adminId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "售后状态已变化，请刷新后重试");
        }
        writeLog(afterSale.getId(), AfterSaleStatus.APPLIED, target,
                AfterSaleOperatorType.ADMIN, adminId, "审核通过");
        notificationService.notifyUser(afterSale.getUserId(),
                "AFTER_SALE_APPROVED",
                "售后申请已通过",
                "售后单 " + afterSale.getAfterSaleNo() + " 已审核通过，"
                        + (target == AfterSaleStatus.WAIT_RETURN ? "请寄回商品并填写退货物流。" : "等待退款处理。"),
                "AFTER_SALE", afterSale.getAfterSaleNo(),
                Map.of("afterSaleNo", afterSale.getAfterSaleNo(), "status", target.name()));
        operationLogger.log(adminId, "AFTER_SALE_APPROVE", "AFTER_SALE",
                afterSale.getAfterSaleNo(), target.name());
        return toDetail(requireById(afterSale.getId()));
    }

    @Transactional
    public AfterSaleDetailVO reject(Long adminId, String afterSaleNo, String reason) {
        AfterSaleOrder afterSale = requireByNo(afterSaleNo);
        if (AfterSaleStatus.REJECTED.name().equals(afterSale.getStatus())) {
            return toDetail(afterSale);
        }
        if (!AfterSaleStatus.APPLIED.name().equals(afterSale.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "当前售后状态不能拒绝");
        }
        String normalized = reason == null || reason.isBlank() ? "不符合售后条件" : reason.trim();
        int updated = afterSaleOrderMapper.reject(afterSale.getId(), adminId, normalized);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "售后状态已变化，请刷新后重试");
        }
        writeLog(afterSale.getId(), AfterSaleStatus.APPLIED, AfterSaleStatus.REJECTED,
                AfterSaleOperatorType.ADMIN, adminId, normalized);
        notificationService.notifyUser(afterSale.getUserId(),
                "AFTER_SALE_REJECTED",
                "售后申请未通过",
                "售后单 " + afterSale.getAfterSaleNo() + " 未通过审核：" + normalized,
                "AFTER_SALE", afterSale.getAfterSaleNo(),
                Map.of("afterSaleNo", afterSale.getAfterSaleNo(), "reason", normalized));
        operationLogger.log(adminId, "AFTER_SALE_REJECT", "AFTER_SALE",
                afterSale.getAfterSaleNo(), normalized);
        return toDetail(requireById(afterSale.getId()));
    }

    @Transactional
    public AfterSaleDetailVO submitReturn(Long userId,
                                          String afterSaleNo,
                                          String company,
                                          String trackingNo) {
        AfterSaleOrder afterSale = requireOwned(userId, afterSaleNo);
        if (AfterSaleStatus.RETURNED.name().equals(afterSale.getStatus())) {
            return toDetail(afterSale);
        }
        if (!AfterSaleStatus.WAIT_RETURN.name().equals(afterSale.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "当前售后状态不能填写退货物流");
        }
        int updated = afterSaleOrderMapper.submitReturn(afterSale.getId(), company.trim(), trackingNo.trim());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "售后状态已变化，请刷新后重试");
        }
        writeLog(afterSale.getId(), AfterSaleStatus.WAIT_RETURN, AfterSaleStatus.RETURNED,
                AfterSaleOperatorType.USER, userId, "用户已寄回：" + company.trim() + " " + trackingNo.trim());
        notificationService.notifyAdmins(
                "AFTER_SALE_RETURNED",
                "用户已寄回商品",
                "售后单 " + afterSale.getAfterSaleNo() + " 已寄回，物流："
                        + company.trim() + " " + trackingNo.trim(),
                "AFTER_SALE", afterSale.getAfterSaleNo(),
                Map.of("afterSaleNo", afterSale.getAfterSaleNo()));
        return toDetail(requireById(afterSale.getId()));
    }

    @Transactional
    public AfterSaleDetailVO confirmReturn(Long adminId, String afterSaleNo) {
        AfterSaleOrder afterSale = requireByNo(afterSaleNo);
        if (AfterSaleStatus.RETURN_RECEIVED.name().equals(afterSale.getStatus())) {
            return toDetail(afterSale);
        }
        if (!AfterSaleStatus.RETURNED.name().equals(afterSale.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "当前售后状态不能确认收货");
        }
        int updated = afterSaleOrderMapper.confirmReturn(afterSale.getId());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "售后状态已变化，请刷新后重试");
        }
        writeLog(afterSale.getId(), AfterSaleStatus.RETURNED, AfterSaleStatus.RETURN_RECEIVED,
                AfterSaleOperatorType.ADMIN, adminId, "确认收到退货");
        notificationService.notifyUser(afterSale.getUserId(),
                "AFTER_SALE_RETURN_RECEIVED",
                "退货已签收",
                "售后单 " + afterSale.getAfterSaleNo() + " 的退货已签收，等待退款。",
                "AFTER_SALE", afterSale.getAfterSaleNo(),
                Map.of("afterSaleNo", afterSale.getAfterSaleNo()));
        operationLogger.log(adminId, "AFTER_SALE_RETURN_RECEIVED", "AFTER_SALE",
                afterSale.getAfterSaleNo(), "确认收到退货");
        return toDetail(requireById(afterSale.getId()));
    }

    @Transactional
    public AfterSaleDetailVO refund(Long adminId, String afterSaleNo, AdminRefundRequest request) {
        AfterSaleOrder afterSale = requireByNo(afterSaleNo);
        if (AfterSaleStatus.REFUNDED.name().equals(afterSale.getStatus())) {
            return toDetail(afterSale);
        }

        boolean returnRefund = AfterSaleType.RETURN_REFUND.name().equals(afterSale.getType());
        List<String> allowed = returnRefund
                ? List.of(AfterSaleStatus.RETURN_RECEIVED.name(), AfterSaleStatus.REFUND_FAILED.name())
                : List.of(AfterSaleStatus.APPROVED.name(), AfterSaleStatus.REFUND_FAILED.name());
        if (!allowed.contains(afterSale.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "当前售后状态不能退款");
        }

        String manualRefundNo = request == null ? null : trimToNull(request.manualRefundNo());
        RefundGateway gateway = refundGateway.orElse(null);
        if (gateway == null && manualRefundNo == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "当前未配置退款渠道，请填写线下退款凭证");
        }

        int updated = afterSaleOrderMapper.startRefunding(afterSale.getId(), afterSale.getStatus());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "售后状态已变化，请刷新后重试");
        }
        writeLog(afterSale.getId(), AfterSaleStatus.from(afterSale.getStatus()),
                AfterSaleStatus.REFUNDING, AfterSaleOperatorType.ADMIN, adminId, "发起退款");

        String channel = gateway == null ? RefundChannel.MANUAL.name() : gateway.channel();
        RefundRecord record = refundRecordMapper.selectOne(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getAfterSaleId, afterSale.getId())
                .last("LIMIT 1"));
        if (record == null) {
            record = new RefundRecord();
            record.setRefundNo("RF" + IdWorker.getIdStr());
            record.setAfterSaleId(afterSale.getId());
            record.setOrderId(afterSale.getOrderId());
            record.setOrderNo(afterSale.getOrderNo());
            record.setChannel(channel);
            record.setAmount(afterSale.getRefundAmount());
            record.setRequestedAt(LocalDateTime.now());
            record.setStatus(RefundStatus.PENDING.name());
            refundRecordMapper.insert(record);
        } else {
            record.setChannel(channel);
            record.setStatus(RefundStatus.PENDING.name());
            record.setRequestedAt(LocalDateTime.now());
            record.setFailedReason(null);
            refundRecordMapper.updateById(record);
        }

        RefundResult result;
        if (gateway != null) {
            try {
                result = gateway.refund(new RefundRequest(
                        afterSale.getId(),
                        afterSale.getAfterSaleNo(),
                        afterSale.getOrderNo(),
                        afterSale.getRefundAmount(),
                        afterSale.getReason()));
            } catch (Exception ex) {
                log.warn("refund gateway failed for {}", afterSale.getAfterSaleNo(), ex);
                result = new RefundResult(false, null, "退款渠道调用失败");
            }
        } else {
            result = new RefundResult(true, manualRefundNo, "线下退款已登记");
        }

        if (!result.success()) {
            record.setStatus(RefundStatus.FAILED.name());
            record.setFailedReason(result.message() == null ? "退款失败" : result.message());
            refundRecordMapper.updateById(record);
            afterSaleOrderMapper.markRefundFailed(afterSale.getId());
            writeLog(afterSale.getId(), AfterSaleStatus.REFUNDING, AfterSaleStatus.REFUND_FAILED,
                    AfterSaleOperatorType.PAYMENT, adminId, result.message());
            return toDetail(requireById(afterSale.getId()));
        }

        LocalDateTime now = LocalDateTime.now();
        record.setStatus(RefundStatus.SUCCESS.name());
        record.setTransactionNo(result.transactionNo());
        record.setSuccessAt(now);
        refundRecordMapper.updateById(record);

        afterSaleOrderMapper.markRefunded(afterSale.getId());

        String paymentStatus = afterSale.getRefundAmount().compareTo(
                orderMapper.selectById(afterSale.getOrderId()).getPayableAmount()) >= 0
                ? PaymentStatus.REFUNDED.name() : PaymentStatus.PARTIALLY_REFUNDED.name();
        orderMapper.updatePaymentStatus(afterSale.getOrderId(), paymentStatus);
        if (PaymentStatus.REFUNDED.name().equals(paymentStatus)) {
            pointsService.revokeOrderPoints(afterSale.getOrderNo(), afterSale.getUserId());
        }

        if (returnRefund) {
            List<AfterSaleItem> items = afterSaleItemMapper.selectList(
                            new LambdaQueryWrapper<AfterSaleItem>()
                                    .eq(AfterSaleItem::getAfterSaleId, afterSale.getId()))
                    .stream()
                    .sorted(Comparator.comparing(AfterSaleItem::getSkuId))
                    .toList();
            for (AfterSaleItem item : items) {
                stockService.restore(
                        item.getSkuId(),
                        item.getQuantity() == null ? 0 : item.getQuantity(),
                        StockChangeType.AFTER_SALE_RESTORE,
                        "AFTER_SALE",
                        afterSale.getAfterSaleNo(),
                        AfterSaleOperatorType.ADMIN.name(),
                        adminId,
                        "退货入库");
            }
        }

        writeLog(afterSale.getId(), AfterSaleStatus.REFUNDING, AfterSaleStatus.REFUNDED,
                AfterSaleOperatorType.PAYMENT, adminId, "退款成功：" + result.transactionNo());
        notificationService.notifyUser(afterSale.getUserId(),
                "REFUND_SUCCESS",
                "退款成功",
                "售后单 " + afterSale.getAfterSaleNo() + " 已退款 "
                        + afterSale.getRefundAmount().toPlainString() + " 元，请留意到账通知。",
                "AFTER_SALE", afterSale.getAfterSaleNo(),
                Map.of("afterSaleNo", afterSale.getAfterSaleNo(),
                        "amount", afterSale.getRefundAmount()));
        operationLogger.log(adminId, "AFTER_SALE_REFUND", "AFTER_SALE",
                afterSale.getAfterSaleNo(),
                channel + " " + afterSale.getRefundAmount().toPlainString());
        return toDetail(requireById(afterSale.getId()));
    }

    private PageResult<AfterSaleSummaryVO> toSummaryPage(Page<AfterSaleOrder> result,
                                                         long page,
                                                         long pageSize) {
        List<AfterSaleOrder> orders = result.getRecords();
        if (orders.isEmpty()) {
            return PageResult.of(List.of(), page, pageSize, result.getTotal());
        }
        Map<Long, List<AfterSaleItem>> itemsByAfterSale = afterSaleItemMapper.selectList(
                        new LambdaQueryWrapper<AfterSaleItem>()
                                .in(AfterSaleItem::getAfterSaleId,
                                        orders.stream().map(AfterSaleOrder::getId).toList()))
                .stream()
                .collect(Collectors.groupingBy(AfterSaleItem::getAfterSaleId,
                        LinkedHashMap::new, Collectors.toList()));
        List<AfterSaleSummaryVO> items = orders.stream().map(afterSale -> {
            List<AfterSaleItem> afterSaleItems =
                    itemsByAfterSale.getOrDefault(afterSale.getId(), List.of());
            int quantity = afterSaleItems.stream()
                    .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity()).sum();
            AfterSaleItem first = afterSaleItems.isEmpty() ? null : afterSaleItems.get(0);
            return new AfterSaleSummaryVO(
                    afterSale.getAfterSaleNo(),
                    afterSale.getOrderNo(),
                    afterSale.getType(),
                    afterSale.getStatus(),
                    afterSale.getRefundAmount(),
                    first == null ? "" : first.getProductName(),
                    first == null ? "" : first.getProductImage(),
                    quantity,
                    afterSale.getCreatedAt());
        }).toList();
        return PageResult.of(items, page, pageSize, result.getTotal());
    }

    private AfterSaleDetailVO toDetail(AfterSaleOrder afterSale) {
        List<AfterSaleItemVO> items = afterSaleItemMapper.selectList(
                        new LambdaQueryWrapper<AfterSaleItem>()
                                .eq(AfterSaleItem::getAfterSaleId, afterSale.getId())
                                .orderByAsc(AfterSaleItem::getId))
                .stream()
                .map(item -> new AfterSaleItemVO(
                        item.getOrderItemId(),
                        item.getProductId(),
                        item.getSkuId(),
                        item.getProductName(),
                        item.getSkuSpec(),
                        item.getProductImage(),
                        item.getUnitPrice(),
                        item.getQuantity() == null ? 0 : item.getQuantity(),
                        item.getRefundAmount()))
                .toList();
        List<AfterSaleLogVO> logs = afterSaleLogMapper.selectList(
                        new LambdaQueryWrapper<AfterSaleLog>()
                                .eq(AfterSaleLog::getAfterSaleId, afterSale.getId())
                                .orderByAsc(AfterSaleLog::getCreatedAt)
                                .orderByAsc(AfterSaleLog::getId))
                .stream()
                .map(log -> new AfterSaleLogVO(
                        log.getFromStatus(),
                        log.getToStatus(),
                        log.getOperatorType(),
                        log.getRemark(),
                        log.getCreatedAt()))
                .toList();
        RefundRecord record = refundRecordMapper.selectOne(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getAfterSaleId, afterSale.getId())
                .orderByDesc(RefundRecord::getId)
                .last("LIMIT 1"));
        RefundInfoVO refund = record == null ? null : new RefundInfoVO(
                record.getRefundNo(),
                record.getChannel(),
                record.getAmount(),
                record.getStatus(),
                record.getTransactionNo(),
                record.getSuccessAt(),
                record.getFailedReason());

        return new AfterSaleDetailVO(
                afterSale.getAfterSaleNo(),
                afterSale.getOrderNo(),
                afterSale.getType(),
                afterSale.getStatus(),
                afterSale.getReason(),
                afterSale.getDescription(),
                afterSale.getRefundAmount(),
                afterSale.getReturnCompany(),
                afterSale.getReturnTrackingNo(),
                afterSale.getReviewRemark(),
                afterSale.getReviewedAt(),
                afterSale.getReturnedAt(),
                afterSale.getRefundedAt(),
                afterSale.getCreatedAt(),
                items,
                logs,
                refund);
    }

    private AfterSaleOrder requireOwned(Long userId, String afterSaleNo) {
        AfterSaleOrder afterSale = afterSaleOrderMapper.selectOne(
                new LambdaQueryWrapper<AfterSaleOrder>()
                        .eq(AfterSaleOrder::getAfterSaleNo, afterSaleNo)
                        .eq(AfterSaleOrder::getUserId, userId));
        if (afterSale == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "售后单不存在");
        }
        return afterSale;
    }

    private AfterSaleOrder requireByNo(String afterSaleNo) {
        AfterSaleOrder afterSale = afterSaleOrderMapper.selectOne(
                new LambdaQueryWrapper<AfterSaleOrder>()
                        .eq(AfterSaleOrder::getAfterSaleNo, afterSaleNo));
        if (afterSale == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "售后单不存在");
        }
        return afterSale;
    }

    private AfterSaleOrder requireById(Long id) {
        AfterSaleOrder afterSale = afterSaleOrderMapper.selectById(id);
        if (afterSale == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "售后单不存在");
        }
        return afterSale;
    }

    private AfterSaleOrder latestForOrder(Long orderId) {
        return afterSaleOrderMapper.selectOne(new LambdaQueryWrapper<AfterSaleOrder>()
                .eq(AfterSaleOrder::getOrderId, orderId)
                .orderByDesc(AfterSaleOrder::getId)
                .last("LIMIT 1"));
    }

    private void writeLog(Long afterSaleId,
                          AfterSaleStatus from,
                          AfterSaleStatus to,
                          AfterSaleOperatorType operatorType,
                          Long operatorId,
                          String remark) {
        AfterSaleLog log = new AfterSaleLog();
        log.setAfterSaleId(afterSaleId);
        log.setFromStatus(from == null ? null : from.name());
        log.setToStatus(to.name());
        log.setOperatorType(operatorType.name());
        log.setOperatorId(operatorId);
        log.setRemark(remark == null ? "" : remark);
        afterSaleLogMapper.insert(log);
    }

    private String typeText(AfterSaleType type) {
        return type == AfterSaleType.RETURN_REFUND ? "退货退款" : "仅退款";
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
