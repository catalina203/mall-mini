package com.mallmini.admin.vo;

import java.time.LocalDateTime;

public record AdminOperationLogVO(
        Long id,
        Long adminId,
        String adminUsername,
        String action,
        String targetType,
        String targetId,
        String detail,
        String requestId,
        LocalDateTime createdAt
) {
}
