package com.mallmini.review.vo;

import java.time.LocalDateTime;

public record ReviewVO(
        Long id,
        Long productId,
        Long skuId,
        String userNickname,
        int rating,
        String content,
        boolean anonymous,
        String status,
        String reply,
        LocalDateTime createdAt
) {
}
