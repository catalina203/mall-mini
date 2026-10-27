package com.mallmini.support.vo;

import java.time.LocalDateTime;

public record SupportTicketVO(
        Long id,
        String ticketNo,
        Long userId,
        String userNickname,
        String orderNo,
        String type,
        String subject,
        String content,
        String status,
        String reply,
        LocalDateTime repliedAt,
        LocalDateTime createdAt
) {
}
