package com.mallmini.banner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BannerSaveRequest(
        @Size(max = 64, message = "标题过长")
        String title,

        @NotBlank(message = "图片地址不能为空")
        @Size(max = 512, message = "图片地址过长")
        String imageUrl,

        @NotBlank(message = "跳转类型不能为空")
        @Size(max = 20, message = "跳转类型不合法")
        String linkType,

        @Size(max = 64, message = "跳转目标过长")
        String linkTarget,

        Integer sortOrder,

        String status
) {
}
