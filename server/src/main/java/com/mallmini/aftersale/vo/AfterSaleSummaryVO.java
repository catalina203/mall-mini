package com.mallmini.aftersale.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AfterSaleSummaryVO(
        String afterSaleNo,
        String orderNo,
        String type,
        String status,
        BigDecimal refundAmount,
        String firstProductName,
        String firstProductImage,
        int totalQuantity,
        LocalDateTime createdAt
) {
}
