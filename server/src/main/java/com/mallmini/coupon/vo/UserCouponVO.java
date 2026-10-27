package com.mallmini.coupon.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserCouponVO(
        Long id,
        String couponNo,
        Long templateId,
        String name,
        String type,
        BigDecimal value,
        BigDecimal minAmount,
        String scope,
        Long scopeId,
        String status,
        String orderNo,
        LocalDateTime validFrom,
        LocalDateTime validTo
) {
}
