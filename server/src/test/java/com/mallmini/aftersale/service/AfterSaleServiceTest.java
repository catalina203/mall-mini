package com.mallmini.aftersale.service;

import com.mallmini.admin.service.AdminOperationLogger;
import com.mallmini.aftersale.dto.AdminRefundRequest;
import com.mallmini.aftersale.dto.CreateAfterSaleRequest;
import com.mallmini.aftersale.entity.AfterSaleItem;
import com.mallmini.aftersale.entity.AfterSaleOrder;
import com.mallmini.aftersale.entity.RefundRecord;
import com.mallmini.aftersale.enums.AfterSaleStatus;
import com.mallmini.aftersale.enums.AfterSaleType;
import com.mallmini.aftersale.gateway.RefundGateway;
import com.mallmini.aftersale.mapper.AfterSaleItemMapper;
import com.mallmini.aftersale.mapper.AfterSaleLogMapper;
import com.mallmini.aftersale.mapper.AfterSaleOrderMapper;
import com.mallmini.aftersale.mapper.RefundRecordMapper;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.inventory.service.StockService;
import com.mallmini.member.service.PointsService;
import com.mallmini.notification.service.NotificationService;
import com.mallmini.order.entity.MallOrder;
import com.mallmini.order.entity.MallOrderItem;
import com.mallmini.order.mapper.MallOrderItemMapper;
import com.mallmini.order.mapper.MallOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AfterSaleServiceTest {

    @Mock
    private AfterSaleOrderMapper afterSaleOrderMapper;
    @Mock
    private AfterSaleItemMapper afterSaleItemMapper;
    @Mock
    private AfterSaleLogMapper afterSaleLogMapper;
    @Mock
    private RefundRecordMapper refundRecordMapper;
    @Mock
    private MallOrderMapper orderMapper;
    @Mock
    private MallOrderItemMapper orderItemMapper;
    @Mock
    private StockService stockService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AdminOperationLogger operationLogger;
    @Mock
    private PointsService pointsService;

    private AfterSaleService service;

    @BeforeEach
    void setUp() {
        service = new AfterSaleService(
                afterSaleOrderMapper,
                afterSaleItemMapper,
                afterSaleLogMapper,
                refundRecordMapper,
                orderMapper,
                orderItemMapper,
                stockService,
                notificationService,
                pointsService,
                operationLogger,
                Optional.empty());
    }

    @Test
    void createFullOrderRefundUsesPayableAmount() {
        MallOrder order = paidOrder("PENDING_SHIPMENT");
        AtomicReference<AfterSaleOrder> saved = new AtomicReference<>();
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(afterSaleOrderMapper.selectOne(any())).thenAnswer(invocation -> saved.get());
        when(afterSaleOrderMapper.insert(any(AfterSaleOrder.class))).thenAnswer(invocation -> {
            AfterSaleOrder afterSale = invocation.getArgument(0);
            afterSale.setId(1L);
            saved.set(afterSale);
            return 1;
        });
        when(orderItemMapper.selectList(any())).thenReturn(List.of(
                orderItem(11L, 21L, "49.00", 2)));
        when(afterSaleItemMapper.selectList(any())).thenReturn(List.of());
        when(afterSaleLogMapper.selectList(any())).thenReturn(List.of());

        service.create(1L, "MM-1", new CreateAfterSaleRequest(
                List.of(new CreateAfterSaleRequest.ItemRequest(11L, 2)),
                "REFUND_ONLY",
                "不想要了",
                ""));

        ArgumentCaptor<AfterSaleOrder> captor = ArgumentCaptor.forClass(AfterSaleOrder.class);
        verify(afterSaleOrderMapper).insert(captor.capture());
        assertThat(captor.getValue().getRefundAmount()).isEqualByComparingTo("100.00");
        assertThat(captor.getValue().getStatus()).isEqualTo("APPLIED");
        assertThat(captor.getValue().getType()).isEqualTo("REFUND_ONLY");
        verify(notificationService).notifyAdmins(eq("AFTER_SALE_APPLIED"), any(), any(),
                eq("AFTER_SALE"), any(), any());
    }

    @Test
    void createRejectsReturnRefundBeforeShipment() {
        when(orderMapper.selectOne(any())).thenReturn(paidOrder("PENDING_SHIPMENT"));
        when(afterSaleOrderMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.create(1L, "MM-1", new CreateAfterSaleRequest(
                List.of(new CreateAfterSaleRequest.ItemRequest(11L, 1)),
                "RETURN_REFUND",
                "质量问题",
                "")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能申请仅退款");
    }

    @Test
    void createRejectsDuplicateActiveAfterSale() {
        when(orderMapper.selectOne(any())).thenReturn(paidOrder("PENDING_RECEIPT"));
        AfterSaleOrder active = afterSale(1L, "AS-1", AfterSaleType.REFUND_ONLY, AfterSaleStatus.APPLIED);
        when(afterSaleOrderMapper.selectOne(any())).thenReturn(active);

        assertThatThrownBy(() -> service.create(1L, "MM-1", new CreateAfterSaleRequest(
                List.of(new CreateAfterSaleRequest.ItemRequest(11L, 1)),
                "REFUND_ONLY",
                "不想要了",
                "")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已有进行中的售后");
    }

    @Test
    void createRejectsQuantityAbovePurchased() {
        when(orderMapper.selectOne(any())).thenReturn(paidOrder("PENDING_RECEIPT"));
        when(afterSaleOrderMapper.selectOne(any())).thenReturn(null);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(
                orderItem(11L, 21L, "50.00", 1)));

        assertThatThrownBy(() -> service.create(1L, "MM-1", new CreateAfterSaleRequest(
                List.of(new CreateAfterSaleRequest.ItemRequest(11L, 2)),
                "REFUND_ONLY",
                "不想要了",
                "")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能超过购买数量");
    }

    @Test
    void refundWithoutGatewayRequiresManualReference() {
        AfterSaleOrder approved = afterSale(1L, "AS-1", AfterSaleType.REFUND_ONLY, AfterSaleStatus.APPROVED);
        when(afterSaleOrderMapper.selectOne(any())).thenReturn(approved);

        assertThatThrownBy(() -> service.refund(99L, "AS-1", new AdminRefundRequest(null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("线下退款凭证");
        verify(refundRecordMapper, never()).insert(any(RefundRecord.class));
    }

    @Test
    void partialRefundUpdatesOrderPaymentStatusWithoutRestoringStock() {
        AfterSaleOrder approved = afterSale(1L, "AS-1", AfterSaleType.REFUND_ONLY, AfterSaleStatus.APPROVED);
        approved.setRefundAmount(new BigDecimal("50.00"));
        approved.setOrderId(100L);
        when(afterSaleOrderMapper.selectOne(any())).thenReturn(approved);
        when(afterSaleOrderMapper.selectById(1L)).thenReturn(approved);
        when(afterSaleOrderMapper.startRefunding(eq(1L), any())).thenReturn(1);
        when(refundRecordMapper.selectOne(any())).thenReturn(null);
        when(orderMapper.selectById(100L)).thenReturn(paidOrder("PENDING_RECEIPT"));
        when(afterSaleItemMapper.selectList(any())).thenReturn(List.of());
        when(afterSaleLogMapper.selectList(any())).thenReturn(List.of());

        service.refund(99L, "AS-1", new AdminRefundRequest("OFFLINE-123", "线下退款"));

        verify(orderMapper).updatePaymentStatus(100L, "PARTIALLY_REFUNDED");
        verify(stockService, never()).restore(anyLong(), eq(1), any(), any(), any(), any(),
                anyLong(), any());
        verify(notificationService).notifyUser(eq(1L), eq("REFUND_SUCCESS"), any(), any(),
                eq("AFTER_SALE"), eq("AS-1"), any());
    }

    @Test
    void returnRefundRestoresStockAfterRefund() {
        AfterSaleOrder approved = afterSale(1L, "AS-1", AfterSaleType.RETURN_REFUND,
                AfterSaleStatus.RETURN_RECEIVED);
        approved.setRefundAmount(new BigDecimal("100.00"));
        approved.setOrderId(100L);
        AfterSaleItem item = new AfterSaleItem();
        item.setAfterSaleId(1L);
        item.setSkuId(21L);
        item.setQuantity(1);
        when(afterSaleOrderMapper.selectOne(any())).thenReturn(approved);
        when(afterSaleOrderMapper.selectById(1L)).thenReturn(approved);
        when(afterSaleOrderMapper.startRefunding(eq(1L), any())).thenReturn(1);
        when(refundRecordMapper.selectOne(any())).thenReturn(null);
        when(orderMapper.selectById(100L)).thenReturn(paidOrder("PENDING_RECEIPT"));
        when(afterSaleItemMapper.selectList(any())).thenReturn(List.of(item));
        when(afterSaleLogMapper.selectList(any())).thenReturn(List.of());

        service.refund(99L, "AS-1", new AdminRefundRequest("OFFLINE-123", "退货退款"));

        verify(orderMapper).updatePaymentStatus(100L, "REFUNDED");
        verify(stockService).restore(eq(21L), eq(1), any(), eq("AFTER_SALE"), eq("AS-1"),
                eq("ADMIN"), eq(99L), any());
    }

    private MallOrder paidOrder(String status) {
        MallOrder order = new MallOrder();
        order.setId(100L);
        order.setOrderNo("MM-1");
        order.setUserId(1L);
        order.setStatus(status);
        order.setPaymentStatus("PAID");
        order.setPayableAmount(new BigDecimal("100.00"));
        return order;
    }

    private MallOrderItem orderItem(Long id, Long skuId, String unitPrice, int quantity) {
        MallOrderItem item = new MallOrderItem();
        item.setId(id);
        item.setOrderId(100L);
        item.setProductId(10L);
        item.setSkuId(skuId);
        item.setProductName("样例商品");
        item.setSkuSpec("默认规格");
        item.setProductImage("");
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setQuantity(quantity);
        item.setSubtotal(new BigDecimal(unitPrice).multiply(BigDecimal.valueOf(quantity)));
        return item;
    }

    private AfterSaleOrder afterSale(Long id,
                                     String afterSaleNo,
                                     AfterSaleType type,
                                     AfterSaleStatus status) {
        AfterSaleOrder afterSale = new AfterSaleOrder();
        afterSale.setId(id);
        afterSale.setAfterSaleNo(afterSaleNo);
        afterSale.setOrderId(100L);
        afterSale.setOrderNo("MM-1");
        afterSale.setUserId(1L);
        afterSale.setType(type.name());
        afterSale.setStatus(status.name());
        afterSale.setReason("测试");
        afterSale.setDescription("");
        afterSale.setRefundAmount(new BigDecimal("100.00"));
        return afterSale;
    }
}
