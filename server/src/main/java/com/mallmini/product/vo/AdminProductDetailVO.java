package com.mallmini.product.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AdminProductDetailVO(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String subtitle,
        String description,
        String mainImage,
        String status,
        Integer sortOrder,
        Long shippingTemplateId,
        List<ProductImageVO> images,
        List<AdminSkuVO> skus
) {

    public record AdminSkuVO(
            Long id,
            String skuCode,
            Map<String, String> spec,
            BigDecimal price,
            int stock,
            String status
    ) {
    }
}
