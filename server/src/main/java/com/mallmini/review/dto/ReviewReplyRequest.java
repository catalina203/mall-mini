package com.mallmini.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewReplyRequest(
        @NotBlank(message = "回复内容不能为空")
        @Size(max = 500, message = "回复过长")
        String reply
) {
}
