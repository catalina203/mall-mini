package com.mallmini.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @NotNull(message = "订单明细不能为空")
        Long orderItemId,

        @NotNull(message = "评分不能为空")
        @Min(value = 1, message = "评分至少 1 星")
        @Max(value = 5, message = "评分最多 5 星")
        Integer rating,

        @Size(max = 1000, message = "评价内容过长")
        String content,

        Boolean anonymous
) {
}
