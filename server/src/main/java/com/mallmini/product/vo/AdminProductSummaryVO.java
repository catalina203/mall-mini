package com.mallmini.product.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminProductSummaryVO(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String subtitle,
        String mainImage,
        String status,
        Integer sortOrder,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        int totalStock,
        int skuCount,
        LocalDateTime updatedAt
) {
}
