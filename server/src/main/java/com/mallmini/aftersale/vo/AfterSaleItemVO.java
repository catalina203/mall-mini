package com.mallmini.aftersale.vo;

import java.math.BigDecimal;

public record AfterSaleItemVO(
        Long orderItemId,
        Long productId,
        Long skuId,
        String productName,
        String skuSpec,
        String productImage,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal refundAmount
) {
}
