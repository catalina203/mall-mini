package com.mallmini.aftersale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectAfterSaleRequest(
        @NotBlank(message = "拒绝原因不能为空")
        @Size(max = 255, message = "拒绝原因过长")
        String reason
) {
}
