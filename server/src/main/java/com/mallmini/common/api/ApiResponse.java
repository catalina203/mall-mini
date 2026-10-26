package com.mallmini.common.api;

import com.mallmini.common.web.RequestContext;

public record ApiResponse<T>(String code, String message, T data, String requestId) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "OK", data, RequestContext.requestId());
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>("OK", "OK", null, RequestContext.requestId());
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, message, null, RequestContext.requestId());
    }
}
