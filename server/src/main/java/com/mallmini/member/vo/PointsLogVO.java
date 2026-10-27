package com.mallmini.member.vo;

import java.time.LocalDateTime;

public record PointsLogVO(
        Long id,
        String changeType,
        int pointsDelta,
        int balanceAfter,
        String remark,
        LocalDateTime createdAt
) {
}
