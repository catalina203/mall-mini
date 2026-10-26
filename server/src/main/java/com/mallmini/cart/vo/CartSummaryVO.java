package com.mallmini.cart.vo;

import java.math.BigDecimal;

public record CartSummaryVO(
        int selectedItemCount,
        int selectedQuantity,
        BigDecimal selectedAmount
) {
}
