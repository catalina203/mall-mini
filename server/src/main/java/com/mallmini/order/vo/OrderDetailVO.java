package com.mallmini.order.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailVO(
        String orderNo,
        String status,
        String paymentStatus,
        BigDecimal goodsAmount,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal payableAmount,
        AddressSnapshotVO address,
        List<OrderItemVO> items,
        String remark,
        String logisticsCompany,
        String logisticsNo,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime shippedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        String cancelReason,
        List<OrderStatusLogVO> statusLogs
) {
}
