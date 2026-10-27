package com.mallmini.coupon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponTemplateSaveRequest(
        @NotBlank(message = "优惠券名称不能为空")
        @Size(max = 64, message = "名称过长")
        String name,

        @NotBlank(message = "类型不能为空")
        String type,

        @NotNull(message = "面额/折扣不能为空")
        @DecimalMin(value = "0.01", message = "面额/折扣必须大于 0")
        BigDecimal value,

        @NotNull(message = "门槛不能为空")
        @DecimalMin(value = "0.00", message = "门槛不能为负")
        BigDecimal minAmount,

        @NotBlank(message = "适用范围不能为空")
        String scope,

        Long scopeId,

        @NotNull(message = "发放总量不能为空")
        @Min(value = 0, message = "发放总量不能为负")
        Integer totalCount,

        @NotNull(message = "每人限领不能为空")
        @Min(value = 1, message = "每人限领至少 1")
        @Max(value = 1, message = "首版每人限领 1 张")
        Integer perUserLimit,

        @NotNull(message = "生效时间不能为空")
        LocalDateTime validFrom,

        @NotNull(message = "失效时间不能为空")
        LocalDateTime validTo,

        String status
) {
}
