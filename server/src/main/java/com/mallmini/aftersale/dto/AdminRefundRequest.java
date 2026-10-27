package com.mallmini.aftersale.dto;

import jakarta.validation.constraints.Size;

public record AdminRefundRequest(
        @Size(max = 64, message = "线下退款凭证过长")
        String manualRefundNo,

        @Size(max = 255, message = "备注过长")
        String remark
) {
}
