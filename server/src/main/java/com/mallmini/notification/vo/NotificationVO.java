package com.mallmini.notification.vo;

import java.time.LocalDateTime;

public record NotificationVO(
        Long id,
        String templateCode,
        String title,
        String content,
        String bizType,
        String bizNo,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
}
