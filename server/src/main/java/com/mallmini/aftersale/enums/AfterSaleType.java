package com.mallmini.aftersale.enums;

import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;

import java.util.Locale;

public enum AfterSaleType {
    REFUND_ONLY,
    RETURN_REFUND;

    public static AfterSaleType from(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "售后类型不能为空");
        }
        try {
            return AfterSaleType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "售后类型不合法");
        }
    }
}
