package com.mallmini.common.api;

import com.mallmini.common.web.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void okCarriesRequestId() {
        MDC.put(RequestContext.REQUEST_ID, "req-123");

        ApiResponse<String> response = ApiResponse.ok("data");

        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.message()).isEqualTo("OK");
        assertThat(response.data()).isEqualTo("data");
        assertThat(response.requestId()).isEqualTo("req-123");
    }

    @Test
    void errorHasNoData() {
        MDC.put(RequestContext.REQUEST_ID, "req-456");

        ApiResponse<Void> response = ApiResponse.error("NOT_FOUND", "不存在");

        assertThat(response.code()).isEqualTo("NOT_FOUND");
        assertThat(response.message()).isEqualTo("不存在");
        assertThat(response.data()).isNull();
        assertThat(response.requestId()).isEqualTo("req-456");
    }
}
