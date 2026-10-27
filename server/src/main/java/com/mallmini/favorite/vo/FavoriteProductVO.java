package com.mallmini.favorite.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FavoriteProductVO(
        Long productId,
        String name,
        String subtitle,
        String mainImage,
        BigDecimal minPrice,
        String status,
        LocalDateTime at
) {
}
