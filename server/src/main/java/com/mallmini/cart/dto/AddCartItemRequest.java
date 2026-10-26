package com.mallmini.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCartItemRequest(
        @NotNull(message = "skuId 不能为空")
        @Positive(message = "skuId 不合法")
        Long skuId,

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为 1")
        @Max(value = 99, message = "单项数量不能超过 99")
        Integer quantity
) {
}
