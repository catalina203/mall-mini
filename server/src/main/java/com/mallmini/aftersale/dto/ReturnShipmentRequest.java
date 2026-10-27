package com.mallmini.aftersale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReturnShipmentRequest(
        @NotBlank(message = "退货物流公司不能为空")
        @Size(max = 64, message = "物流公司过长")
        String company,

        @NotBlank(message = "退货物流单号不能为空")
        @Size(max = 64, message = "物流单号过长")
        String trackingNo
) {
}
