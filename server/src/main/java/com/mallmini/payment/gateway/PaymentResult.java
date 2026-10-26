package com.mallmini.payment.gateway;

public record PaymentResult(
        boolean success,
        String transactionNo,
        String message
) {
}
