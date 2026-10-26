package com.mallmini.cart.vo;

import java.math.BigDecimal;
import java.util.Map;

public record CartItemVO(
        Long id,
        Long skuId,
        Long productId,
        String productName,
        String productImage,
        Map<String, String> spec,
        BigDecimal price,
        int stock,
        int quantity,
        boolean selected,
        boolean available,
        String unavailableReason,
        BigDecimal subtotal
) {
}
