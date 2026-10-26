package com.mallmini.payment.vo;

public record PaymentConfigVO(
        boolean mockEnabled,
        String channel
) {
}
