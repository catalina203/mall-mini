package com.mallmini.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CheckoutPreviewRequest(
        @NotNull(message = "请选择收货地址")
        Long addressId,

        @Size(max = 20, message = "下单来源不合法")
        String source,

        @Size(max = 100, message = "购物车项过多")
        List<Long> cartItemIds,

        Long skuId,

        @Min(value = 1, message = "数量至少为 1")
        @Max(value = 99, message = "单项数量不能超过 99")
        Integer quantity,

        Long couponId
) {
}
