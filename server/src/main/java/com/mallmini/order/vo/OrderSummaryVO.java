package com.mallmini.order.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryVO(
        String orderNo,
        String status,
        String paymentStatus,
        BigDecimal payableAmount,
        int totalQuantity,
        String firstProductName,
        String firstProductImage,
        LocalDateTime createdAt
) {
}
