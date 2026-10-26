package com.mallmini.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductSaveRequest(
        @NotBlank(message = "商品名称不能为空")
        @Size(max = 128, message = "商品名称过长")
        String name,

        @Size(max = 255, message = "副标题过长")
        String subtitle,

        String description,

        @NotNull(message = "请选择分类")
        Long categoryId,

        @Size(max = 512, message = "主图地址过长")
        String mainImage,

        Integer sortOrder,

        String status,

        List<ProductImageItem> images,

        @NotEmpty(message = "至少需要一个 SKU")
        List<SkuSaveItem> skus
) {

    public record ProductImageItem(
            @NotBlank(message = "图片地址不能为空")
            @Size(max = 512, message = "图片地址过长")
            String url,

            Integer sortOrder
    ) {
    }

    public record SkuSaveItem(
            Long id,

            @NotBlank(message = "SKU 编码不能为空")
            @Size(max = 64, message = "SKU 编码过长")
            String skuCode,

            Map<String, String> spec,

            @NotNull(message = "价格不能为空")
            @DecimalMin(value = "0.00", message = "价格不能为负")
            BigDecimal price,

            @NotNull(message = "库存不能为空")
            @Min(value = 0, message = "库存不能为负")
            @Max(value = 99999999, message = "库存过大")
            Integer stock,

            String status
    ) {
    }
}
