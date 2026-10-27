package com.mallmini.payment.service;

import com.mallmini.common.exception.BusinessException;
import com.mallmini.config.AppProperties;
import com.mallmini.notification.service.NotificationService;
import com.mallmini.order.entity.MallOrder;
import com.mallmini.order.mapper.MallOrderItemMapper;
import com.mallmini.product.mapper.ProductMapper;
import com.mallmini.order.entity.OrderStatusLog;
import com.mallmini.order.mapper.MallOrderMapper;
import com.mallmini.order.mapper.OrderStatusLogMapper;
import com.mallmini.order.service.OrderService;
import com.mallmini.order.vo.OrderDetailVO;
import com.mallmini.payment.entity.PaymentRecord;
import com.mallmini.payment.gateway.PaymentGateway;
import com.mallmini.payment.gateway.PaymentRequest;
import com.mallmini.payment.gateway.PaymentResult;
import com.mallmini.payment.mapper.PaymentRecordMapper;
import com.mallmini.payment.vo.PaymentResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private MallOrderMapper orderMapper;
    @Mock
    private PaymentRecordMapper paymentRecordMapper;
    @Mock
    private OrderStatusLogMapper statusLogMapper;
    @Mock
    private OrderService orderService;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private NotificationService notificationService;
    @Mock
    private MallOrderItemMapper orderItemMapper;
    @Mock
    private ProductMapper productMapper;

    private AppProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        properties.getPayment().setMockEnabled(true);
    }

    @Test
    void mockPaymentCompletesOrderAndWritesRecords() {
        MallOrder order = order("PENDING_PAYMENT", "UNPAID");
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(paymentGateway.channel()).thenReturn("MOCK");
        when(paymentGateway.pay(any(PaymentRequest.class)))
                .thenReturn(new PaymentResult(true, "MOCK-TX-1", "模拟支付成功"));
        when(orderMapper.transitionToPaid(1L)).thenReturn(1);
        when(orderService.detail(1L, "MM-1")).thenReturn(mock(OrderDetailVO.class));

        PaymentResultVO result = service(Optional.of(paymentGateway)).pay(1L, "MM-1");

        assertThat(result.mock()).isTrue();
        assertThat(result.channel()).isEqualTo("MOCK");
        verify(paymentRecordMapper).insert(any(PaymentRecord.class));
        verify(statusLogMapper).insert(any(OrderStatusLog.class));
    }

    @Test
    void alreadyPaidOrderIsReturnedWithoutPayingAgain() {
        MallOrder order = order("PENDING_SHIPMENT", "PAID");
        PaymentRecord record = new PaymentRecord();
        record.setChannel("MOCK");
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(paymentRecordMapper.selectOne(any())).thenReturn(record);
        when(orderService.detail(1L, "MM-1")).thenReturn(mock(OrderDetailVO.class));

        PaymentResultVO result = service(Optional.of(paymentGateway)).pay(1L, "MM-1");

        assertThat(result.mock()).isTrue();
        verify(paymentGateway, never()).pay(any(PaymentRequest.class));
        verify(orderMapper, never()).transitionToPaid(anyLong());
    }

    @Test
    void cancelledOrderCannotBePaid() {
        when(orderMapper.selectOne(any())).thenReturn(order("CANCELLED", "UNPAID"));

        assertThatThrownBy(() -> service(Optional.of(paymentGateway)).pay(1L, "MM-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已取消");
        verify(paymentGateway, never()).pay(any(PaymentRequest.class));
    }

    @Test
    void paymentDisabledWhenNoGateway() {
        when(orderMapper.selectOne(any())).thenReturn(order("PENDING_PAYMENT", "UNPAID"));

        assertThatThrownBy(() -> service(Optional.empty()).pay(1L, "MM-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未启用");
    }

    @Test
    void failedGatewayReturnsPaymentError() {
        when(orderMapper.selectOne(any())).thenReturn(order("PENDING_PAYMENT", "UNPAID"));
        when(paymentGateway.pay(any(PaymentRequest.class)))
                .thenReturn(new PaymentResult(false, null, "渠道失败"));

        assertThatThrownBy(() -> service(Optional.of(paymentGateway)).pay(1L, "MM-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("渠道失败");
        verify(orderMapper, never()).transitionToPaid(anyLong());
        verify(paymentRecordMapper, never()).insert(any(PaymentRecord.class));
    }

    @Test
    void invalidOrderStateCannotBePaid() {
        when(orderMapper.selectOne(any())).thenReturn(order("PENDING_SHIPMENT", "UNPAID"));

        assertThatThrownBy(() -> service(Optional.of(paymentGateway)).pay(1L, "MM-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可支付");
        verify(paymentGateway, never()).pay(any(PaymentRequest.class));
    }

    private PaymentService service(Optional<PaymentGateway> gateway) {
        return new PaymentService(orderMapper, paymentRecordMapper, statusLogMapper,
                orderItemMapper, productMapper, orderService, gateway, properties,
                notificationService);
    }

    private MallOrder order(String status, String paymentStatus) {
        MallOrder order = new MallOrder();
        order.setId(1L);
        order.setOrderNo("MM-1");
        order.setUserId(1L);
        order.setStatus(status);
        order.setPaymentStatus(paymentStatus);
        order.setPayableAmount(new BigDecimal("100.00"));
        return order;
    }
}
