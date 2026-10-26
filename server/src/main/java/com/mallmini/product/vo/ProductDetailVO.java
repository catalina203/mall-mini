package com.mallmini.product.vo;

import java.util.List;

public record ProductDetailVO(
        Long id,
        String name,
        String subtitle,
        String description,
        String mainImage,
        Long categoryId,
        String status,
        List<ProductImageVO> images,
        List<SkuVO> skus
) {
}
