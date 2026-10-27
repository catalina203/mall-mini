package com.mallmini.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUpdateRequest(
        @Size(max = 64, message = "显示名称过长")
        String displayName,

        @NotBlank(message = "角色不能为空")
        String role
) {
}
