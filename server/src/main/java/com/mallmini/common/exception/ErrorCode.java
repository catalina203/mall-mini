package com.mallmini.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "参数校验失败"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "未登录或登录已过期"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "没有操作权限"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "资源不存在"),
    CONFLICT(HttpStatus.CONFLICT, "数据冲突"),
    STOCK_INSUFFICIENT(HttpStatus.CONFLICT, "库存不足"),
    ORDER_STATE_INVALID(HttpStatus.CONFLICT, "订单状态不允许该操作"),
    PRODUCT_UNAVAILABLE(HttpStatus.CONFLICT, "商品已下架或规格失效"),
    ADDRESS_UNAVAILABLE(HttpStatus.CONFLICT, "收货地址不可用"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "操作过于频繁，请稍后再试"),
    PAYMENT_DISABLED(HttpStatus.FORBIDDEN, "当前环境未启用该支付方式"),
    PAYMENT_FAILED(HttpStatus.CONFLICT, "支付失败"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "服务暂不可用"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
