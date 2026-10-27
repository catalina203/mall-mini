package com.mallmini.order.vo;

import java.math.BigDecimal;

public record OrderItemVO(
        Long productId,
        Long skuId,
        String productName,
        String skuSpec,
        String productImage,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal,
        Long orderItemId
) {
}
