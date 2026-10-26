package com.mallmini.payment.gateway;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MockPaymentGatewayTest {

    @Test
    void returnsSuccessfulMockTransaction() {
        MockPaymentGateway gateway = new MockPaymentGateway();

        PaymentResult result = gateway.pay(new PaymentRequest(1L, "MM-1", new BigDecimal("99.00")));

        assertThat(gateway.channel()).isEqualTo("MOCK");
        assertThat(result.success()).isTrue();
        assertThat(result.transactionNo()).startsWith("MOCK");
    }
}
