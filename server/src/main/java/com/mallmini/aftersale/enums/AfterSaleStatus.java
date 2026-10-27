package com.mallmini.aftersale.enums;

import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;

import java.util.Locale;

public enum AfterSaleStatus {
    APPLIED,
    APPROVED,
    WAIT_RETURN,
    RETURNED,
    RETURN_RECEIVED,
    REFUNDING,
    REFUNDED,
    REFUND_FAILED,
    REJECTED,
    CANCELLED;

    public static AfterSaleStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "售后状态不能为空");
        }
        try {
            return AfterSaleStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "售后状态不合法");
        }
    }

    public boolean isActive() {
        return this == APPLIED || this == APPROVED || this == WAIT_RETURN
                || this == RETURNED || this == RETURN_RECEIVED
                || this == REFUNDING || this == REFUND_FAILED;
    }
}
