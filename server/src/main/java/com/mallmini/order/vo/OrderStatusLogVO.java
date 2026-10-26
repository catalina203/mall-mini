package com.mallmini.order.vo;

import java.time.LocalDateTime;

public record OrderStatusLogVO(
        String fromStatus,
        String toStatus,
        String operatorType,
        String remark,
        LocalDateTime createdAt
) {
}
