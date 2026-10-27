package com.mallmini.aftersale.gateway;

import java.math.BigDecimal;

public record RefundRequest(
        Long afterSaleId,
        String afterSaleNo,
        String orderNo,
        BigDecimal amount,
        String reason
) {
}
