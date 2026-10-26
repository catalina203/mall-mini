package com.mallmini.category.vo;

public record AdminCategoryVO(
        Long id,
        Long parentId,
        String parentName,
        String name,
        String iconUrl,
        Integer sortOrder,
        String status
) {
}
