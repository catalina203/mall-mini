package com.mallmini.aftersale.gateway;

public record RefundResult(
        boolean success,
        String transactionNo,
        String message
) {
}
