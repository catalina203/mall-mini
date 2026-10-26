package com.mallmini.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mallmini.address.entity.UserAddress;
import com.mallmini.address.service.AddressService;
import com.mallmini.admin.service.AdminOperationLogger;
import com.mallmini.cart.mapper.CartItemMapper;
import com.mallmini.category.service.CategoryService;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.config.AppProperties;
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
import static org.mockito.ArgumentMatchers.eq;
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
    private ProductMapper productMapper;
    @Mock
    private CategoryService categoryService;
    @Mock
    private AddressService addressService;
    @Mock
    private CartItemMapper cartItemMapper;
    @Mock
    private AdminOperationLogger operationLogger;

    private AppProperties properties;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        properties.getShipping().setFee(new BigDecimal("10.00"));
        properties.getShipping().setFreeThreshold(new BigDecimal("99.00"));
        properties.getOrder().setCancelMinutes(30);
        orderService = new OrderService(
                orderMapper,
                orderItemMapper,
                statusLogMapper,
                productSkuMapper,
                productMapper,
                categoryService,
                addressService,
                cartItemMapper,
                new SpecJsonParser(new ObjectMapper()),
                properties,
                operationLogger);
    }

    @Test
    void previewChargesShippingBelowThreshold() {
        mockPurchasableSku("60.00", 10);
        when(addressService.requireOwned(1L, 1L)).thenReturn(address());

        CheckoutVO checkout = orderService.preview(1L,
                new CheckoutPreviewRequest(1L, "BUY_NOW", null, 1L, 1));

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

        CheckoutVO checkout = orderService.preview(1L,
                new CheckoutPreviewRequest(1L, "BUY_NOW", null, 1L, 2));

        assertThat(checkout.goodsAmount()).isEqualByComparingTo("120.00");
        assertThat(checkout.shippingFee()).isEqualByComparingTo("0.00");
        assertThat(checkout.payableAmount()).isEqualByComparingTo("120.00");
    }

    @Test
    void previewRejectsInsufficientStock() {
        mockPurchasableSku("60.00", 1);
        when(addressService.requireOwned(1L, 1L)).thenReturn(address());

        assertThatThrownBy(() -> orderService.preview(1L,
                new CheckoutPreviewRequest(1L, "BUY_NOW", null, 1L, 2)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存不足");
    }

    @Test
    void createOrderRecalculatesPriceAndDeductsStock() {
        mockPurchasableSku("60.00", 10);
        when(addressService.requireOwned(1L, 1L)).thenReturn(address());
        when(orderMapper.selectOne(any())).thenReturn(null);
        when(orderMapper.insert(any(MallOrder.class))).thenAnswer(invocation -> {
            MallOrder order = invocation.getArgument(0);
            order.setId(100L);
            return 1;
        });
        when(productSkuMapper.deductStock(1L, 2)).thenReturn(1);

        OrderDetailVO detail = orderService.create(1L,
                new CreateOrderRequest(1L, "BUY_NOW", null, 1L, 2, "req-1", ""));

        assertThat(detail.orderNo()).startsWith("MM");
        assertThat(detail.status()).isEqualTo("PENDING_PAYMENT");
        assertThat(detail.payableAmount()).isEqualByComparingTo("120.00");
        verify(productSkuMapper).deductStock(1L, 2);
        verify(orderItemMapper).insert(any(MallOrderItem.class));
        verify(statusLogMapper).insert(any(OrderStatusLog.class));
    }

    @Test
    void duplicateRequestReturnsExistingOrderWithoutDeductingStock() {
        MallOrder existing = order(1L, "MM-EXISTING", "PENDING_PAYMENT");
        when(orderMapper.selectOne(any())).thenReturn(existing);

        OrderDetailVO detail = orderService.create(1L,
                new CreateOrderRequest(1L, "BUY_NOW", null, 1L, 1, "req-1", ""));

        assertThat(detail.orderNo()).isEqualTo("MM-EXISTING");
        verify(orderMapper, never()).insert(any(MallOrder.class));
        verify(productSkuMapper, never()).deductStock(anyLong(), anyInt());
    }

    @Test
    void cancelRestoresStockAndIsIdempotent() {
        MallOrder pending = order(1L, "MM-1", "PENDING_PAYMENT");
        MallOrder cancelled = order(1L, "MM-1", "CANCELLED");
        when(orderMapper.selectOne(any())).thenReturn(pending);
        when(orderMapper.transitionToCancelled(1L, "PENDING_PAYMENT", "不想要了")).thenReturn(1);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(orderItem(1L, 1L, 2)));
        when(productSkuMapper.restoreStock(1L, 2)).thenReturn(1);
        when(orderMapper.selectById(1L)).thenReturn(cancelled);

        orderService.cancel(1L, "MM-1", "不想要了");

        verify(productSkuMapper).restoreStock(1L, 2);
        verify(statusLogMapper).insert(any(OrderStatusLog.class));
    }

    @Test
    void cancellingAlreadyCancelledOrderDoesNotRestoreAgain() {
        MallOrder cancelled = order(1L, "MM-1", "CANCELLED");
        when(orderMapper.selectOne(any())).thenReturn(cancelled);

        orderService.cancel(1L, "MM-1", "再次取消");

        verify(productSkuMapper, never()).restoreStock(anyLong(), anyInt());
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
    void adminCancelRestoresStock() {
        MallOrder pending = order(1L, "MM-1", "PENDING_PAYMENT");
        MallOrder cancelled = order(1L, "MM-1", "CANCELLED");
        when(orderMapper.selectOne(any())).thenReturn(pending);
        when(orderMapper.transitionToCancelled(1L, "PENDING_PAYMENT", "管理员取消")).thenReturn(1);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(orderItem(1L, 1L, 2)));
        when(productSkuMapper.restoreStock(1L, 2)).thenReturn(1);
        when(orderMapper.selectById(1L)).thenReturn(cancelled);

        orderService.adminCancel("MM-1", "管理员取消", 99L);

        verify(productSkuMapper).restoreStock(1L, 2);
        verify(operationLogger).log(eq(99L), eq("ORDER_CANCEL"), eq("ORDER"), eq("MM-1"), eq("管理员取消"));
    }

    @Test
    void systemCancelRestoresStockOnce() {        MallOrder pending = order(1L, "MM-1", "PENDING_PAYMENT");
        MallOrder cancelled = order(1L, "MM-1", "CANCELLED");
        when(orderMapper.selectById(1L)).thenReturn(pending, cancelled);
        when(orderMapper.transitionToCancelled(eq(1L), eq("PENDING_PAYMENT"), eq("超时未支付"))).thenReturn(1);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(orderItem(1L, 1L, 3)));
        when(productSkuMapper.restoreStock(1L, 3)).thenReturn(1);

        orderService.systemCancel(1L);
        orderService.systemCancel(1L);

        verify(productSkuMapper).restoreStock(1L, 3);
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
