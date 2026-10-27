package com.mallmini.dashboard.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DashboardTrendRow {

    private LocalDate day;

    private Long orderCount;

    private BigDecimal paidAmount;
}
