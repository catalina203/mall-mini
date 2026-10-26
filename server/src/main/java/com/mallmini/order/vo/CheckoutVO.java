package com.mallmini.order.vo;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutVO(
        AddressSnapshotVO address,
        List<OrderItemVO> items,
        BigDecimal goodsAmount,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal payableAmount,
        BigDecimal freeShippingThreshold
) {
}
