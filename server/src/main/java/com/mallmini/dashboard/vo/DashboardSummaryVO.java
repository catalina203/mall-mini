package com.mallmini.dashboard.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DashboardSummaryVO(
        int days,
        LocalDateTime rangeStart,
        LocalDateTime rangeEnd,
        LocalDateTime generatedAt,
        long orderCount,
        BigDecimal paidAmount,
        long pendingShipmentCount,
        long pendingReceiptCount,
        long todayOrderCount
) {
}
