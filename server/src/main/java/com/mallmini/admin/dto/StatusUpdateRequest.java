package com.mallmini.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StatusUpdateRequest(
        @NotBlank(message = "状态不能为空")
        @Size(max = 32, message = "状态不合法")
        String status
) {
}
