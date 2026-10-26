package com.mallmini.payment.vo;

import com.mallmini.order.vo.OrderDetailVO;

public record PaymentResultVO(
        boolean mock,
        String channel,
        OrderDetailVO order
) {
}
