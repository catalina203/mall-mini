package com.mallmini.order.dto;

import jakarta.validation.constraints.Size;

public record CancelOrderRequest(
        @Size(max = 255, message = "取消原因过长")
        String reason
) {
}
