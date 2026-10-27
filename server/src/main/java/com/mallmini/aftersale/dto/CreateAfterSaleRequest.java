package com.mallmini.aftersale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateAfterSaleRequest(
        @NotEmpty(message = "请选择售后商品")
        @Valid
        List<ItemRequest> items,

        @NotBlank(message = "售后类型不能为空")
        String type,

        @NotBlank(message = "售后原因不能为空")
        @Size(max = 255, message = "售后原因过长")
        String reason,

        @Size(max = 1000, message = "问题描述过长")
        String description
) {

    public record ItemRequest(
            @NotNull(message = "订单明细不能为空")
            Long orderItemId,

            @NotNull(message = "售后数量不能为空")
            @Min(value = 1, message = "售后数量必须大于 0")
            Integer quantity
    ) {
    }
}
