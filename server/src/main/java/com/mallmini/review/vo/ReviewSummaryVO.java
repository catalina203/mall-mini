package com.mallmini.review.vo;

import java.math.BigDecimal;

public record ReviewSummaryVO(
        Long productId,
        BigDecimal averageRating,
        long reviewCount
) {
}
