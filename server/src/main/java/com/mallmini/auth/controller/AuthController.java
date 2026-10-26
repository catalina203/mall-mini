package com.mallmini.auth.controller;

import com.mallmini.auth.dto.DevLoginRequest;
import com.mallmini.auth.dto.WechatLoginRequest;
import com.mallmini.auth.service.AuthService;
import com.mallmini.auth.vo.AuthTokenVO;
import com.mallmini.common.api.ApiResponse;
import com.mallmini.common.ratelimit.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final int LOGIN_LIMIT_PER_MINUTE = 10;

    private final AuthService authService;
    private final RateLimitService rateLimitService;

    @PostMapping("/dev-login")
    public ApiResponse<AuthTokenVO> devLogin(@Valid @RequestBody DevLoginRequest request,
                                             HttpServletRequest httpRequest) {
        rateLimitService.check("login:" + clientIp(httpRequest), LOGIN_LIMIT_PER_MINUTE, 60);
        return ApiResponse.ok(authService.devLogin(request));
    }

    @PostMapping("/wechat-login")
    public ApiResponse<AuthTokenVO> wechatLogin(@Valid @RequestBody WechatLoginRequest request,
                                                HttpServletRequest httpRequest) {
        rateLimitService.check("login:" + clientIp(httpRequest), LOGIN_LIMIT_PER_MINUTE, 60);
        return ApiResponse.ok(authService.wechatLogin(request));
    }

    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
