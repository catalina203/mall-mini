package com.mallmini.member.controller;

import com.mallmini.common.api.ApiResponse;
import com.mallmini.common.api.PageResult;
import com.mallmini.member.service.PointsService;
import com.mallmini.member.vo.MemberVO;
import com.mallmini.member.vo.PointsLogVO;
import com.mallmini.security.AuthUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MemberController {

    private final PointsService pointsService;

    @GetMapping("/member")
    public ApiResponse<MemberVO> member(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(pointsService.member(authUser.id()));
    }

    @GetMapping("/points/logs")
    public ApiResponse<PageResult<PointsLogVO>> logs(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize) {
        return ApiResponse.ok(pointsService.logs(authUser.id(), page, pageSize));
    }
}
