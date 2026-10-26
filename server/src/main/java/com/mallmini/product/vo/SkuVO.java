package com.mallmini.product.vo;

import java.math.BigDecimal;
import java.util.Map;

public record SkuVO(
        Long id,
        String skuCode,
        Map<String, String> spec,
        BigDecimal price,
        int stock,
        String status
) {
}
