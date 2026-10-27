package com.mallmini.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.config.AppProperties;
import com.mallmini.notification.service.NotificationService;
import com.mallmini.order.entity.MallOrder;
import com.mallmini.order.entity.MallOrderItem;
import com.mallmini.order.entity.OrderStatusLog;
import com.mallmini.order.enums.OrderOperatorType;
import com.mallmini.order.enums.OrderStatus;
import com.mallmini.order.enums.PaymentStatus;
import com.mallmini.order.mapper.MallOrderItemMapper;
import com.mallmini.order.mapper.MallOrderMapper;
import com.mallmini.order.mapper.OrderStatusLogMapper;
import com.mallmini.order.service.OrderService;
import com.mallmini.payment.entity.PaymentRecord;
import com.mallmini.payment.enums.PaymentRecordStatus;
import com.mallmini.payment.gateway.PaymentGateway;
import com.mallmini.payment.gateway.PaymentRequest;
import com.mallmini.payment.gateway.PaymentResult;
import com.mallmini.payment.mapper.PaymentRecordMapper;
import com.mallmini.payment.vo.PaymentConfigVO;
import com.mallmini.payment.vo.PaymentResultVO;
import com.mallmini.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final MallOrderMapper orderMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final MallOrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final OrderService orderService;
    private final Optional<PaymentGateway> paymentGateway;
    private final AppProperties properties;
    private final NotificationService notificationService;

    public PaymentConfigVO config() {
        String channel = paymentGateway.map(PaymentGateway::channel).orElse("");
        return new PaymentConfigVO(
                properties.getPayment().isMockEnabled() && paymentGateway.isPresent(),
                channel);
    }

    @Transactional
    public PaymentResultVO pay(Long userId, String orderNo) {
        MallOrder order = orderMapper.selectOne(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getOrderNo, orderNo)
                .eq(MallOrder::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        if (OrderStatus.CANCELLED.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "已取消订单不能支付");
        }
        if (PaymentStatus.PAID.name().equals(order.getPaymentStatus())) {
            return paidResult(userId, orderNo);
        }
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus())
                || !PaymentStatus.UNPAID.name().equals(order.getPaymentStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "当前订单状态不可支付");
        }

        PaymentGateway gateway = paymentGateway.orElseThrow(
                () -> new BusinessException(ErrorCode.PAYMENT_DISABLED));
        PaymentResult result = gateway.pay(new PaymentRequest(
                order.getId(), order.getOrderNo(), order.getPayableAmount()));
        if (!result.success()) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED,
                    result.message() == null ? "支付失败" : result.message());
        }

        int updated = orderMapper.transitionToPaid(order.getId());
        if (updated == 0) {
            MallOrder latest = orderMapper.selectById(order.getId());
            if (latest != null && PaymentStatus.PAID.name().equals(latest.getPaymentStatus())) {
                return paidResult(userId, orderNo);
            }
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "订单状态已变化，请刷新后重试");
        }

        PaymentRecord record = new PaymentRecord();
        record.setPaymentNo("PAY" + IdWorker.getIdStr());
        record.setOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setChannel(gateway.channel());
        record.setAmount(order.getPayableAmount());
        record.setStatus(PaymentRecordStatus.SUCCESS.name());
        record.setTransactionNo(result.transactionNo());
        record.setPaidAt(LocalDateTime.now());
        paymentRecordMapper.insert(record);

        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(order.getId());
        log.setFromStatus(OrderStatus.PENDING_PAYMENT.name());
        log.setToStatus(OrderStatus.PENDING_SHIPMENT.name());
        log.setOperatorType(OrderOperatorType.PAYMENT.name());
        log.setRemark("MOCK".equals(gateway.channel()) ? "模拟支付成功" : "支付成功");
        statusLogMapper.insert(log);
        List<MallOrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<MallOrderItem>()
                        .eq(MallOrderItem::getOrderId, order.getId()));
        for (MallOrderItem item : items) {
            productMapper.incrementSales(item.getProductId(),
                    item.getQuantity() == null ? 0 : item.getQuantity());
        }
        notificationService.notifyUser(
                order.getUserId(),
                "ORDER_PAID",
                "支付成功",
                "订单 " + order.getOrderNo() + " 已支付 "
                        + order.getPayableAmount().toPlainString() + " 元。",
                "ORDER",
                order.getOrderNo(),
                Map.of("orderNo", order.getOrderNo(),
                        "amount", order.getPayableAmount()));

        return new PaymentResultVO(
                "MOCK".equals(gateway.channel()),
                gateway.channel(),
                orderService.detail(userId, orderNo));
    }

    private PaymentResultVO paidResult(Long userId, String orderNo) {
        PaymentRecord latest = paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderNo, orderNo)
                .orderByDesc(PaymentRecord::getId)
                .last("LIMIT 1"));
        String channel = latest == null ? "" : latest.getChannel();
        return new PaymentResultVO(
                "MOCK".equals(channel),
                channel,
                orderService.detail(userId, orderNo));
    }
}
