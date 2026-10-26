package com.mallmini.payment.gateway;

import java.math.BigDecimal;

public record PaymentRequest(
        Long orderId,
        String orderNo,
        BigDecimal amount
) {
}
