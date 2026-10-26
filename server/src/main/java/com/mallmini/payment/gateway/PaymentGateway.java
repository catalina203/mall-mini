package com.mallmini.payment.gateway;

public interface PaymentGateway {

    String channel();

    PaymentResult pay(PaymentRequest request);
}
