package com.mallmini.dashboard.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardSummaryVO(
        int days,
        LocalDateTime rangeStart,
        LocalDateTime rangeEnd,
        LocalDateTime generatedAt,
        long orderCount,
        long paidOrderCount,
        BigDecimal paidAmount,
        BigDecimal refundAmount,
        BigDecimal netAmount,
        BigDecimal averageOrderValue,
        BigDecimal refundRate,
        long pendingShipmentCount,
        long pendingReceiptCount,
        long pendingAfterSaleCount,
        long lowStockCount,
        long todayOrderCount,
        List<DashboardTrendVO> trend
) {
}
