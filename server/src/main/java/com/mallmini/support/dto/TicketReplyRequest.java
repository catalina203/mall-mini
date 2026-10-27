package com.mallmini.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketReplyRequest(
        @NotBlank(message = "回复内容不能为空")
        @Size(max = 2000, message = "回复过长")
        String reply
) {
}
