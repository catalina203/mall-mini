package com.mallmini.order.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminOrderSummaryVO(
        String orderNo,
        Long userId,
        String status,
        String paymentStatus,
        BigDecimal payableAmount,
        int totalQuantity,
        String receiverName,
        String receiverPhone,
        LocalDateTime createdAt
) {
}
