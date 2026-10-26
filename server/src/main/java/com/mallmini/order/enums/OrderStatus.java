package com.mallmini.order.enums;

import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;

import java.util.Locale;

public enum OrderStatus {
    PENDING_PAYMENT,
    PENDING_SHIPMENT,
    PENDING_RECEIPT,
    COMPLETED,
    CANCELLED;

    public static OrderStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "订单状态不合法");
        }
        try {
            return OrderStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "订单状态不合法");
        }
    }
}
