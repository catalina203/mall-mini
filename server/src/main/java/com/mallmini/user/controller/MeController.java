package com.mallmini.user.controller;

import com.mallmini.common.api.ApiResponse;
import com.mallmini.security.AuthUser;
import com.mallmini.user.service.UserService;
import com.mallmini.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserVO> me(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(userService.getProfile(authUser.id()));
    }
}
