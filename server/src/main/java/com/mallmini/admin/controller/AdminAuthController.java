package com.mallmini.admin.controller;

import com.mallmini.admin.dto.AdminLoginRequest;
import com.mallmini.admin.dto.AdminPasswordChangeRequest;
import com.mallmini.admin.service.AdminAuthService;
import com.mallmini.admin.service.AdminManagementService;
import com.mallmini.admin.vo.AdminTokenVO;
import com.mallmini.admin.vo.AdminVO;
import com.mallmini.common.api.ApiResponse;
import com.mallmini.common.ratelimit.RateLimitService;
import com.mallmini.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private static final int LOGIN_LIMIT_PER_MINUTE = 10;

    private final AdminAuthService adminAuthService;
    private final AdminManagementService adminManagementService;
    private final RateLimitService rateLimitService;

    @PostMapping("/auth/login")
    public ApiResponse<AdminTokenVO> login(@Valid @RequestBody AdminLoginRequest request,
                                           HttpServletRequest httpRequest) {
        rateLimitService.check("admin-login:" + httpRequest.getRemoteAddr(), LOGIN_LIMIT_PER_MINUTE, 60);
        return ApiResponse.ok(adminAuthService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<AdminVO> me(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(adminAuthService.me(authUser.id()));
    }

    @PostMapping("/auth/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal AuthUser authUser,
                                            @Valid @RequestBody AdminPasswordChangeRequest request) {
        adminManagementService.changePassword(authUser.id(), request.oldPassword(), request.newPassword());
        return ApiResponse.ok();
    }
}
