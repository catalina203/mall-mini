package com.mallmini.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mallmini.address.entity.UserAddress;
import com.mallmini.address.service.AddressService;
import com.mallmini.admin.service.AdminOperationLogger;
import com.mallmini.aftersale.entity.AfterSaleOrder;
import com.mallmini.aftersale.mapper.AfterSaleOrderMapper;
import com.mallmini.cart.mapper.CartItemMapper;
import com.mallmini.category.service.CategoryService;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.config.AppProperties;
import com.mallmini.coupon.service.CouponService;
import com.mallmini.inventory.enums.StockChangeType;
import com.mallmini.inventory.service.StockService;
import com.mallmini.member.service.PointsService;
import com.mallmini.notification.service.NotificationService;
import com.mallmini.order.dto.CheckoutPreviewRequest;
import com.mallmini.order.dto.CreateOrderRequest;
import com.mallmini.order.entity.MallOrder;
import com.mallmini.order.entity.MallOrderItem;
import com.mallmini.order.entity.OrderStatusLog;
import com.mallmini.order.mapper.MallOrderItemMapper;
import com.mallmini.order.mapper.MallOrderMapper;
import com.mallmini.order.mapper.OrderStatusLogMapper;
import com.mallmini.order.vo.CheckoutVO;
import com.mallmini.order.vo.OrderDetailVO;
import com.mallmini.product.entity.Product;
import com.mallmini.product.entity.ProductSku;
import com.mallmini.product.mapper.ProductMapper;
import com.mallmini.product.mapper.ProductSkuMapper;
import com.mallmini.product.service.SpecJsonParser;
import com.mallmini.shipping.service.ShippingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private MallOrderMapper orderMapper;
    @Mock
    private MallOrderItemMapper orderItemMapper;
    @Mock
    private OrderStatusLogMapper statusLogMapper;
    @Mock
    private ProductSkuMapper productSkuMapper;
    @Mock
    private StockService stockService;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private CategoryService categoryService;
    @Mock
    private AddressService addressService;
    @Mock
    private CartItemMapper cartItemMapper;
    @Mock
    private AdminOperationLogger operationLogger;
    @Mock
    private AfterSaleOrderMapper afterSaleOrderMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ShippingService shippingService;
    @Mock
    private CouponService couponService;
    @Mock
    private PointsService pointsService;

    private AppProperties properties;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        properties.getShipping().setFee(new BigDecimal("10.00"));
        properties.getShipping().setFreeThreshold(new BigDecimal("99.00"));
        properties.getOrder().setCancelMinutes(30);
        lenient().when(couponService.calculate(any(), any(), any(), any()))
                .thenReturn(CouponService.CouponApplication.none());
        lenient().when(shippingService.calculate(any(), any())).thenReturn(BigDecimal.ZERO);
        orderService = new OrderService(
                orderMapper,
                orderItemMapper,
                statusLogMapper,
                productSkuMapper,
                stockService,
                productMapper,
                categoryService,
                addressService,
                cartItemMapper,
                new SpecJsonParser(new ObjectMapper()),
                properties,
                operationLogger,
                afterSaleOrderMapper,
                notificationService,
                shippingService,
                couponService,
                pointsService);
    }

    @Test
    void previewChargesShippingBelowThreshold() {
        mockPurchasableSku("60.00", 10);
        when(addressService.requireOwned(1L, 1L)).thenReturn(address());

        when(shippingService.calculate(anyString(), any())).thenReturn(new BigDecimal("10.00"));
        CheckoutVO checkout = orderService.preview(1L,
                new CheckoutPreviewRequest(1L, "BUY_NOW", null, 1L, 1, null));

        assertThat(checkout.goodsAmount()).isEqualByComparingTo("60.00");
        assertThat(checkout.shippingFee()).isEqualByComparingTo("10.00");
        assertThat(checkout.discountAmount()).isEqualByComparingTo("0.00");
        assertThat(checkout.payableAmount()).isEqualByComparingTo("70.00");
        assertThat(checkout.items()).hasSize(1);
    }

    @Test
    void previewFreeShippingAtThreshold() {
        mockPurchasableSku("60.00", 10);
        when(addressService.requireOwned(1L, 1L)).thenReturn(address());

        when(shippingService.calculate(anyString(), any())).thenReturn(BigDecimal.ZERO);
        CheckoutVO checkout = orderService.preview(1L,
                new CheckoutPreviewRequest(1L, "BUY_NOW", null, 1L, 2, null));

        assertThat(checkout.goodsAmount()).isEqualByComparingTo("120.00");
        assertThat(checkout.shippingFee()).isEqualByComparingTo("0.00");
        assertThat(checkout.payableAmount()).isEqualByComparingTo("120.00");
    }

    @Test
    void previewRejectsInsufficientStock() {
        mockPurchasableSku("60.00", 1);
        when(addressService.requireOwned(1L, 1L)).thenReturn(address());

        assertThatThrownBy(() -> orderService.preview(1L,
                new CheckoutPreviewRequest(1L, "BUY_NOW", null, 1L, 2, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存不足");
    }

    @Test
    void createOrderRecalculatesPriceAndDeductsStock() {
        mockPurchasableSku("60.00", 10);
        when(addressService.requireOwned(1L, 1L)).thenReturn(address());
        when(orderMapper.selectOne(any())).thenReturn(null);
        when(shippingService.calculate(anyString(), any())).thenReturn(BigDecimal.ZERO);
        when(orderMapper.insert(any(MallOrder.class))).thenAnswer(invocation -> {
            MallOrder order = invocation.getArgument(0);
            order.setId(100L);
            return 1;
        });

        OrderDetailVO detail = orderService.create(1L,
                new CreateOrderRequest(1L, "BUY_NOW", null, 1L, 2, "req-1", "", null, null, null, null, null));

        assertThat(detail.orderNo()).startsWith("MM");
        assertThat(detail.status()).isEqualTo("PENDING_PAYMENT");
        assertThat(detail.payableAmount()).isEqualByComparingTo("120.00");
        verify(stockService).deduct(eq(1L), eq(2), eq("ORDER"), anyString(),
                eq("USER"), eq(1L), anyString());
        verify(orderItemMapper).insert(any(MallOrderItem.class));
        verify(statusLogMapper).insert(any(OrderStatusLog.class));
    }

    @Test
    void duplicateRequestReturnsExistingOrderWithoutDeductingStock() {
        MallOrder existing = order(1L, "MM-EXISTING", "PENDING_PAYMENT");
        when(orderMapper.selectOne(any())).thenReturn(existing);

        OrderDetailVO detail = orderService.create(1L,
                new CreateOrderRequest(1L, "BUY_NOW", null, 1L, 1, "req-1", "", null, null, null, null, null));

        assertThat(detail.orderNo()).isEqualTo("MM-EXISTING");
        verify(orderMapper, never()).insert(any(MallOrder.class));
        verify(stockService, never()).deduct(anyLong(), anyInt(), anyString(), anyString(),
                anyString(), anyLong(), anyString());
    }

    @Test
    void cancelRestoresStockAndIsIdempotent() {
        MallOrder pending = order(1L, "MM-1", "PENDING_PAYMENT");
        MallOrder cancelled = order(1L, "MM-1", "CANCELLED");
        when(orderMapper.selectOne(any())).thenReturn(pending);
        when(orderMapper.transitionToCancelled(1L, "PENDING_PAYMENT", "不想要了")).thenReturn(1);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(orderItem(1L, 1L, 2)));
        when(orderMapper.selectById(1L)).thenReturn(cancelled);

        orderService.cancel(1L, "MM-1", "不想要了");

        verify(stockService).restore(eq(1L), eq(2), eq(StockChangeType.ORDER_CANCEL_RESTORE),
                eq("ORDER"), anyString(), eq("USER"), eq(1L), anyString());
        verify(statusLogMapper).insert(any(OrderStatusLog.class));
    }

    @Test
    void cancellingAlreadyCancelledOrderDoesNotRestoreAgain() {
        MallOrder cancelled = order(1L, "MM-1", "CANCELLED");
        when(orderMapper.selectOne(any())).thenReturn(cancelled);

        orderService.cancel(1L, "MM-1", "再次取消");

        verify(stockService, never()).restore(anyLong(), anyInt(), any(), anyString(),
                anyString(), anyString(), anyLong(), anyString());
        verify(orderMapper, never()).transitionToCancelled(anyLong(), any(), any());
    }

    @Test
    void cancelRejectsExpiredOrder() {
        MallOrder pending = order(1L, "MM-1", "PENDING_PAYMENT");
        pending.setCreatedAt(LocalDateTime.now().minusMinutes(60));
        when(orderMapper.selectOne(any())).thenReturn(pending);

        assertThatThrownBy(() -> orderService.cancel(1L, "MM-1", "太晚了"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超过取消时限");
        verify(orderMapper, never()).transitionToCancelled(anyLong(), any(), any());
    }

    @Test
    void confirmReceiptRejectsWrongState() {
        MallOrder pending = order(1L, "MM-1", "PENDING_PAYMENT");
        when(orderMapper.selectOne(any())).thenReturn(pending);

        assertThatThrownBy(() -> orderService.confirmReceipt(1L, "MM-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可确认收货");
        verify(orderMapper, never()).transitionToCompleted(anyLong());
    }

        @Test
    void adminShipTransitionsToPendingReceipt() {
        MallOrder pending = order(1L, "MM-1", "PENDING_SHIPMENT");
        MallOrder shipped = order(1L, "MM-1", "PENDING_RECEIPT");
        when(orderMapper.selectOne(any())).thenReturn(pending);
        when(orderMapper.transitionToShipped(1L, "顺丰", "SF123")).thenReturn(1);
        when(orderMapper.selectById(1L)).thenReturn(shipped);

        OrderDetailVO detail = orderService.ship("MM-1", "顺丰", "SF123", 99L);

        assertThat(detail.status()).isEqualTo("PENDING_RECEIPT");
        verify(operationLogger).log(eq(99L), eq("ORDER_SHIP"), eq("ORDER"), eq("MM-1"), any());
    }

    @Test
    void adminShipRejectsWrongState() {
        MallOrder pending = order(1L, "MM-1", "PENDING_PAYMENT");
        when(orderMapper.selectOne(any())).thenReturn(pending);

        assertThatThrownBy(() -> orderService.ship("MM-1", "顺丰", "SF123", 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可发货");
        verify(orderMapper, never()).transitionToShipped(anyLong(), any(), any());
    }

    @Test
    void adminShipRejectsRefundedOrder() {
        MallOrder refunded = order(1L, "MM-1", "PENDING_SHIPMENT");
        refunded.setPaymentStatus("REFUNDED");
        when(orderMapper.selectOne(any())).thenReturn(refunded);

        assertThatThrownBy(() -> orderService.ship("MM-1", "顺丰", "SF123", 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已退款");
        verify(orderMapper, never()).transitionToShipped(anyLong(), any(), any());
    }

    @Test
    void adminCancelRestoresStock() {
        MallOrder pending = order(1L, "MM-1", "PENDING_PAYMENT");
        MallOrder cancelled = order(1L, "MM-1", "CANCELLED");
        when(orderMapper.selectOne(any())).thenReturn(pending);
        when(orderMapper.transitionToCancelled(1L, "PENDING_PAYMENT", "管理员取消")).thenReturn(1);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(orderItem(1L, 1L, 2)));
        when(orderMapper.selectById(1L)).thenReturn(cancelled);

        orderService.adminCancel("MM-1", "管理员取消", 99L);

        verify(stockService).restore(eq(1L), eq(2), eq(StockChangeType.ORDER_CANCEL_RESTORE),
                eq("ORDER"), anyString(), eq("ADMIN"), eq(99L), anyString());
        verify(operationLogger).log(eq(99L), eq("ORDER_CANCEL"), eq("ORDER"), eq("MM-1"), eq("管理员取消"));
    }

    @Test
    void systemCancelRestoresStockOnce() {        MallOrder pending = order(1L, "MM-1", "PENDING_PAYMENT");
        MallOrder cancelled = order(1L, "MM-1", "CANCELLED");
        when(orderMapper.selectById(1L)).thenReturn(pending, cancelled);
        when(orderMapper.transitionToCancelled(eq(1L), eq("PENDING_PAYMENT"), eq("超时未支付"))).thenReturn(1);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(orderItem(1L, 1L, 3)));

        orderService.systemCancel(1L);
        orderService.systemCancel(1L);

        verify(stockService).restore(eq(1L), eq(3), eq(StockChangeType.ORDER_CANCEL_RESTORE),
                eq("ORDER"), anyString(), eq("SYSTEM"), isNull(), anyString());
    }

    @Test
    void createOrderRequiresInvoiceTitleWhenRequested() {
        mockPurchasableSku("60.00", 10);
        when(addressService.requireOwned(1L, 1L)).thenReturn(address());
        when(orderMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> orderService.create(1L,
                new CreateOrderRequest(1L, "BUY_NOW", null, 1L, 1, "req-invoice", "",
                        true, "", null, "buyer@example.com", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("发票抬头");
        verify(orderMapper, never()).insert(any(MallOrder.class));
    }

    @Test
    void changeAddressUpdatesSnapshotBeforeShipment() {
        MallOrder pending = order(1L, "MM-1", "PENDING_SHIPMENT");
        pending.setPaymentStatus("PAID");
        when(orderMapper.selectOne(any())).thenReturn(pending);
        when(afterSaleOrderMapper.selectOne(any())).thenReturn(null);
        when(addressService.requireOwned(1L, 2L)).thenReturn(address());
        when(orderMapper.updateReceiver(eq(1L), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(1);
        when(orderMapper.selectById(1L)).thenReturn(pending);
        when(orderItemMapper.selectList(any())).thenReturn(List.of());
        when(statusLogMapper.selectList(any())).thenReturn(List.of());

        orderService.changeAddress(1L, "MM-1", 2L);

        verify(orderMapper).updateReceiver(eq(1L), eq("张三"), eq("13800138000"),
                eq("广东省"), eq("深圳市"), eq("南山区"), eq("科技园路 1 号"));
        verify(statusLogMapper).insert(any(OrderStatusLog.class));
    }

    @Test
    void changeAddressRejectsActiveAfterSale() {
        MallOrder pending = order(1L, "MM-1", "PENDING_SHIPMENT");
        pending.setPaymentStatus("PAID");
        AfterSaleOrder active = new AfterSaleOrder();
        active.setStatus("APPLIED");
        when(orderMapper.selectOne(any())).thenReturn(pending);
        when(afterSaleOrderMapper.selectOne(any())).thenReturn(active);

        assertThatThrownBy(() -> orderService.changeAddress(1L, "MM-1", 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("售后处理中");
        verify(orderMapper, never()).updateReceiver(anyLong(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void autoConfirmCompletesEligibleOrder() {
        MallOrder shipped = order(1L, "MM-1", "PENDING_RECEIPT");
        shipped.setPaymentStatus("PAID");
        shipped.setShippedAt(LocalDateTime.now().minusDays(10));
        when(orderMapper.selectById(1L)).thenReturn(shipped);
        when(afterSaleOrderMapper.selectOne(any())).thenReturn(null);
        when(orderMapper.transitionToCompleted(1L)).thenReturn(1);

        boolean result = orderService.autoConfirmReceipt(1L, 7);

        assertThat(result).isTrue();
        verify(orderMapper).transitionToCompleted(1L);
        verify(statusLogMapper).insert(any(OrderStatusLog.class));
        verify(notificationService).notifyUser(eq(1L), eq("ORDER_AUTO_COMPLETED"),
                any(), any(), eq("ORDER"), eq("MM-1"), any());
    }

    @Test
    void autoConfirmSkipsActiveAfterSale() {
        MallOrder shipped = order(1L, "MM-1", "PENDING_RECEIPT");
        shipped.setPaymentStatus("PAID");
        shipped.setShippedAt(LocalDateTime.now().minusDays(10));
        AfterSaleOrder active = new AfterSaleOrder();
        active.setStatus("WAIT_RETURN");
        when(orderMapper.selectById(1L)).thenReturn(shipped);
        when(afterSaleOrderMapper.selectOne(any())).thenReturn(active);

        boolean result = orderService.autoConfirmReceipt(1L, 7);

        assertThat(result).isFalse();
        verify(orderMapper, never()).transitionToCompleted(anyLong());
    }

    @Test
    void updateAdminRemarkWritesOperationLog() {
        MallOrder shipped = order(1L, "MM-1", "PENDING_SHIPMENT");
        when(orderMapper.selectOne(any())).thenReturn(shipped);
        when(orderMapper.updateAdminRemark(1L, "加急")).thenReturn(1);
        when(orderMapper.selectById(1L)).thenReturn(shipped);
        when(afterSaleOrderMapper.selectOne(any())).thenReturn(null);
        when(orderItemMapper.selectList(any())).thenReturn(List.of());
        when(statusLogMapper.selectList(any())).thenReturn(List.of());

        orderService.updateAdminRemark("MM-1", "加急", 99L);

        verify(orderMapper).updateAdminRemark(1L, "加急");
        verify(operationLogger).log(eq(99L), eq("ORDER_REMARK"), eq("ORDER"), eq("MM-1"), eq("加急"));
    }

    private void mockPurchasableSku(String price, int stock) {
        ProductSku sku = new ProductSku();
        sku.setId(1L);
        sku.setProductId(10L);
        sku.setSkuCode("SKU-1");
        sku.setSpecJson("{\"颜色\":\"黑色\"}");
        sku.setPrice(new BigDecimal(price));
        sku.setStock(stock);
        sku.setStatus("ENABLED");
        when(productSkuMapper.selectById(1L)).thenReturn(sku);

        Product product = new Product();
        product.setId(10L);
        product.setCategoryId(5L);
        product.setName("样例商品");
        product.setMainImage("");
        product.setStatus("ON_SALE");
        when(productMapper.selectById(10L)).thenReturn(product);
        when(categoryService.isEnabled(5L)).thenReturn(true);
    }

    private UserAddress address() {
        UserAddress address = new UserAddress();
        address.setId(1L);
        address.setUserId(1L);
        address.setReceiverName("张三");
        address.setPhone("13800138000");
        address.setProvince("广东省");
        address.setCity("深圳市");
        address.setDistrict("南山区");
        address.setDetail("科技园路 1 号");
        address.setIsDefault(true);
        return address;
    }

    private MallOrder order(Long id, String orderNo, String status) {
        MallOrder order = new MallOrder();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setUserId(1L);
        order.setStatus(status);
        order.setPaymentStatus("UNPAID");
        order.setGoodsAmount(new BigDecimal("100.00"));
        order.setDiscountAmount(new BigDecimal("0.00"));
        order.setShippingFee(new BigDecimal("0.00"));
        order.setPayableAmount(new BigDecimal("100.00"));
        order.setReceiverName("张三");
        order.setReceiverPhone("13800138000");
        order.setReceiverProvince("广东省");
        order.setReceiverCity("深圳市");
        order.setReceiverDistrict("南山区");
        order.setReceiverDetail("科技园路 1 号");
        order.setClientRequestNo("req-1");
        order.setRemark("");
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }

    private MallOrderItem orderItem(Long orderId, Long skuId, int quantity) {
        MallOrderItem item = new MallOrderItem();
        item.setOrderId(orderId);
        item.setOrderNo("MM-1");
        item.setSkuId(skuId);
        item.setProductId(10L);
        item.setProductName("样例商品");
        item.setSkuSpec("默认规格");
        item.setProductImage("");
        item.setUnitPrice(new BigDecimal("60.00"));
        item.setQuantity(quantity);
        item.setSubtotal(new BigDecimal("60.00").multiply(BigDecimal.valueOf(quantity)));
        return item;
    }
}
