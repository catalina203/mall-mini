package com.mallmini.aftersale.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundInfoVO(
        String refundNo,
        String channel,
        BigDecimal amount,
        String status,
        String transactionNo,
        LocalDateTime successAt,
        String failedReason
) {
}
