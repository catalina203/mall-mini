package com.mallmini.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCreateRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名过长")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度需为 8-64 位")
        String password,

        @Size(max = 64, message = "显示名称过长")
        String displayName,

        @NotBlank(message = "角色不能为空")
        String role
) {
}
