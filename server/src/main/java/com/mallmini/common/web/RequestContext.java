package com.mallmini.common.web;

import org.slf4j.MDC;

public final class RequestContext {

    public static final String REQUEST_ID = "requestId";

    private RequestContext() {
    }

    public static String requestId() {
        String value = MDC.get(REQUEST_ID);
        return value == null ? "" : value;
    }
}
