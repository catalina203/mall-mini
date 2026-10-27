package com.mallmini.shipping.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ShippingTemplateSaveRequest(
        @NotBlank(message = "模板名称不能为空")
        @Size(max = 64, message = "模板名称过长")
        String name,

        @NotNull(message = "默认运费不能为空")
        @DecimalMin(value = "0.00", message = "默认运费不能为负")
        BigDecimal defaultFee,

        @NotNull(message = "包邮门槛不能为空")
        @DecimalMin(value = "0.00", message = "包邮门槛不能为负")
        BigDecimal freeThreshold,

        String status,

        List<@Valid RuleItem> rules
) {

    public record RuleItem(
            @NotBlank(message = "地区不能为空，空字符串表示全国")
            @Size(max = 64, message = "地区名称过长")
            String regionCode,

            @NotNull(message = "首件数量不能为空")
            @Min(value = 1, message = "首件数量至少为 1")
            Integer firstUnit,

            @NotNull(message = "首件运费不能为空")
            @DecimalMin(value = "0.00", message = "首件运费不能为负")
            BigDecimal firstFee,

            @NotNull(message = "续件数量不能为空")
            @Min(value = 1, message = "续件数量至少为 1")
            Integer additionalUnit,

            @NotNull(message = "续件运费不能为空")
            @DecimalMin(value = "0.00", message = "续件运费不能为负")
            BigDecimal additionalFee,

            @NotNull(message = "规则包邮门槛不能为空")
            @DecimalMin(value = "0.00", message = "规则包邮门槛不能为负")
            BigDecimal freeThreshold,

            String status,

            Integer sortOrder
    ) {
    }
}
