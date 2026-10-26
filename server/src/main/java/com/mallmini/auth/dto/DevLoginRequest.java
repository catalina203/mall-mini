package com.mallmini.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DevLoginRequest(
        @NotBlank(message = "openId 不能为空")
        @Size(max = 64, message = "openId 过长")
        String openId,

        @Size(max = 64, message = "昵称过长")
        String nickname
) {
}
