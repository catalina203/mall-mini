package com.mallmini.coupon.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponTemplateVO(
        Long id,
        String name,
        String type,
        BigDecimal value,
        BigDecimal minAmount,
        String scope,
        Long scopeId,
        int totalCount,
        int claimedCount,
        int perUserLimit,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        String status,
        LocalDateTime createdAt
) {
}
