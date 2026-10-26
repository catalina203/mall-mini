package com.mallmini.cart.vo;

import java.util.List;

public record CartVO(
        List<CartItemVO> items,
        CartSummaryVO summary
) {
}
