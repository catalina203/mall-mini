package com.mallmini.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(
        @Min(value = 1, message = "数量至少为 1")
        @Max(value = 99, message = "单项数量不能超过 99")
        Integer quantity,

        Boolean selected
) {
}
