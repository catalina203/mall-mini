package com.mallmini.user.controller;

import com.mallmini.admin.dto.StatusUpdateRequest;
import com.mallmini.common.api.ApiResponse;
import com.mallmini.common.api.PageResult;
import com.mallmini.member.dto.PointsAdjustRequest;
import com.mallmini.member.service.PointsService;
import com.mallmini.security.AuthUser;
import com.mallmini.user.service.AdminUserService;
import com.mallmini.user.vo.AdminUserVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final PointsService pointsService;

    @PreAuthorize("hasAuthority('USER_READ')")
    @GetMapping
    public ApiResponse<PageResult<AdminUserVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(adminUserService.page(page, pageSize, keyword, status));
    }

    @PreAuthorize("hasAuthority('USER_STATUS')")
    @PostMapping("/{id}/points")
    public ApiResponse<Void> adjustPoints(@AuthenticationPrincipal AuthUser authUser,
                                          @PathVariable Long id,
                                          @Valid @RequestBody PointsAdjustRequest request) {
        pointsService.adjust(id, request.delta(), request.remark(), authUser.id());
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('USER_STATUS')")
    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@AuthenticationPrincipal AuthUser authUser,
                                          @PathVariable Long id,
                                          @Valid @RequestBody StatusUpdateRequest request) {
        adminUserService.updateStatus(id, request.status(), authUser.id());
        return ApiResponse.ok();
    }
}
