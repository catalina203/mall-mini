package com.mallmini.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @Size(max = 32, message = "订单号过长")
        String orderNo,

        @NotBlank(message = "问题类型不能为空")
        @Size(max = 32, message = "问题类型过长")
        String type,

        @NotBlank(message = "问题标题不能为空")
        @Size(max = 128, message = "标题过长")
        String subject,

        @NotBlank(message = "问题描述不能为空")
        @Size(max = 2000, message = "描述过长")
        String content
) {
}
