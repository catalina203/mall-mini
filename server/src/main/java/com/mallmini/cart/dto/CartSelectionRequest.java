package com.mallmini.cart.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CartSelectionRequest(
        @NotNull(message = "selected 不能为空")
        Boolean selected,

        List<Long> ids
) {
}
