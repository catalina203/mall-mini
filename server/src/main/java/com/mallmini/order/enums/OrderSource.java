package com.mallmini.order.enums;

import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;

import java.util.Locale;

public enum OrderSource {
    CART,
    BUY_NOW;

    public static OrderSource from(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "下单来源不合法");
        }
        try {
            return OrderSource.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "下单来源不合法");
        }
    }
}
