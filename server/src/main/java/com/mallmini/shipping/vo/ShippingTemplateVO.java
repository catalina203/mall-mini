package com.mallmini.shipping.vo;

import java.math.BigDecimal;
import java.util.List;

public record ShippingTemplateVO(
        Long id,
        String name,
        BigDecimal defaultFee,
        BigDecimal freeThreshold,
        String status,
        List<RuleVO> rules
) {
    public record RuleVO(
            Long id,
            String regionCode,
            int firstUnit,
            BigDecimal firstFee,
            int additionalUnit,
            BigDecimal additionalFee,
            BigDecimal freeThreshold,
            String status,
            int sortOrder
    ) {
    }
}
