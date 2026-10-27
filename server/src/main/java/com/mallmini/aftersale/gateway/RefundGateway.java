package com.mallmini.aftersale.gateway;

public interface RefundGateway {

    String channel();

    RefundResult refund(RefundRequest request);
}
