package com.mallmini.order.dto;

import jakarta.validation.constraints.Size;

public record UpdateOrderRemarkRequest(
        @Size(max = 255, message = "备注过长")
        String remark
) {
}
