package com.mallmini.payment.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.payment", name = "mock-enabled", havingValue = "true")
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public String channel() {
        return "MOCK";
    }

    @Override
    public PaymentResult pay(PaymentRequest request) {
        return new PaymentResult(
                true,
                "MOCK" + UUID.randomUUID().toString().replace("-", ""),
                "模拟支付成功");
    }
}
