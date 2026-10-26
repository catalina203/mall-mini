package com.mallmini.health;

import com.mallmini.common.api.ApiResponse;
import com.mallmini.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        boolean databaseUp;
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            databaseUp = true;
        } catch (RuntimeException ex) {
            log.warn("database health check failed: {}", ex.getMessage());
            databaseUp = false;
        }

        if (!databaseUp) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.name(), "数据库不可用"));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("database", "UP");
        data.put("time", OffsetDateTime.now().toString());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
