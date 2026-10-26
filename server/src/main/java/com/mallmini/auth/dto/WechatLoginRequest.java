package com.mallmini.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WechatLoginRequest(
        @NotBlank(message = "code 不能为空")
        @Size(max = 128, message = "code 过长")
        String code
) {
}
