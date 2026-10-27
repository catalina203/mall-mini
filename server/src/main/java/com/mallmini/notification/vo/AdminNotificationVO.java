package com.mallmini.notification.vo;

import java.time.LocalDateTime;

public record AdminNotificationVO(
        Long id,
        String channel,
        String recipientType,
        Long recipientId,
        String templateCode,
        String title,
        String content,
        String bizType,
        String bizNo,
        String status,
        int retryCount,
        String errorMessage,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {
}
