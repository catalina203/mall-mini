package com.mallmini.user.vo;

import java.time.LocalDateTime;

public record AdminUserVO(
        Long id,
        String nickname,
        String avatarUrl,
        String phone,
        String status,
        int points,
        String memberLevel,
        LocalDateTime createdAt
) {
}
