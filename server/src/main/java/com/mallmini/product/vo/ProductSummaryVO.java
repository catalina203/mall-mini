package com.mallmini.product.vo;

import java.math.BigDecimal;

public record ProductSummaryVO(
        Long id,
        String name,
        String subtitle,
        String mainImage,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        int totalStock
) {
}
