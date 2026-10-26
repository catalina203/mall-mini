package com.mallmini.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotNull(message = "请选择收货地址")
        Long addressId,

        @NotBlank(message = "下单来源不能为空")
        @Size(max = 20, message = "下单来源不合法")
        String source,

        @Size(max = 100, message = "购物车项过多")
        List<Long> cartItemIds,

        Long skuId,

        @Min(value = 1, message = "数量至少为 1")
        @Max(value = 99, message = "单项数量不能超过 99")
        Integer quantity,

        @NotBlank(message = "请求标识不能为空")
        @Size(max = 64, message = "请求标识过长")
        String clientRequestNo,

        @Size(max = 255, message = "备注过长")
        String remark
) {
}
