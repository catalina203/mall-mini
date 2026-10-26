package com.mallmini.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockUpdateRequest(
        @NotNull(message = "库存不能为空")
        @Min(value = 0, message = "库存不能为负")
        @Max(value = 99999999, message = "库存过大")
        Integer stock
) {
}
