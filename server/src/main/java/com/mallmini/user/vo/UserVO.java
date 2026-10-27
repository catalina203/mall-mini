package com.mallmini.user.vo;

public record UserVO(
        Long id,
        String nickname,
        String avatarUrl,
        String phone,
        String status,
        int points,
        String memberLevel,
        String memberLevelLabel
) {
}
