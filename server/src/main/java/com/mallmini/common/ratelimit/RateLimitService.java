package com.mallmini.common.ratelimit;

import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitService {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public void check(String key, int limit, long windowSeconds) {
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || now - current.startMillis >= windowSeconds * 1000) {
                return new Window(now, 1);
            }
            return new Window(current.startMillis, current.count + 1);
        });
        if (window.count > limit) {
            throw new BusinessException(ErrorCode.RATE_LIMITED);
        }
    }

    private record Window(long startMillis, int count) {
    }
}
