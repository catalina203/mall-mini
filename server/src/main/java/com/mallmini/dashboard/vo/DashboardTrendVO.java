package com.mallmini.dashboard.vo;

import java.math.BigDecimal;

public record DashboardTrendVO(
        String date,
        long orderCount,
        BigDecimal paidAmount
) {
}
