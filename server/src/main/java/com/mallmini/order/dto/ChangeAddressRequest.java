package com.mallmini.order.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeAddressRequest(
        @NotNull(message = "请选择收货地址")
        Long addressId
) {
}
