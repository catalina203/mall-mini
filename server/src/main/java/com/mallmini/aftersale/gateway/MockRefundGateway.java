package com.mallmini.aftersale.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.payment", name = "mock-enabled", havingValue = "true")
public class MockRefundGateway implements RefundGateway {

    @Override
    public String channel() {
        return "MOCK";
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        return new RefundResult(
                true,
                "MOCKREFUND" + UUID.randomUUID().toString().replace("-", ""),
                "模拟退款成功");
    }
}
