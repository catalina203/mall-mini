package com.mallmini.inventory.vo;

import java.math.BigDecimal;

public record StockAlertVO(
        Long skuId,
        Long productId,
        String skuCode,
        String productName,
        String specText,
        int stock,
        int threshold,
        BigDecimal price
) {
}
