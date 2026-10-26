package com.mallmini.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategorySaveRequest(
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 64, message = "分类名称过长")
        String name,

        Long parentId,

        @Size(max = 512, message = "图标地址过长")
        String iconUrl,

        Integer sortOrder,

        String status
) {
}
