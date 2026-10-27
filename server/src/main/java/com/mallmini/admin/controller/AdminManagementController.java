package com.mallmini.admin.controller;

import com.mallmini.admin.dto.AdminCreateRequest;
import com.mallmini.admin.dto.AdminPasswordResetRequest;
import com.mallmini.admin.dto.AdminUpdateRequest;
import com.mallmini.admin.dto.StatusUpdateRequest;
import com.mallmini.admin.service.AdminManagementService;
import com.mallmini.admin.vo.AdminVO;
import com.mallmini.common.api.ApiResponse;
import com.mallmini.common.api.PageResult;
import com.mallmini.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/admin/admins")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN_MANAGE')")
public class AdminManagementController {

    private final AdminManagementService adminManagementService;

    @GetMapping
    public ApiResponse<PageResult<AdminVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(adminManagementService.page(page, pageSize, keyword, status));
    }

    @GetMapping("/roles")
    public ApiResponse<List<String>> roles() {
        return ApiResponse.ok(adminManagementService.roles());
    }

    @PostMapping
    public ApiResponse<AdminVO> create(@AuthenticationPrincipal AuthUser authUser,
                                       @Valid @RequestBody AdminCreateRequest request) {
        return ApiResponse.ok(adminManagementService.create(request, authUser.id()));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminVO> update(@AuthenticationPrincipal AuthUser authUser,
                                       @PathVariable Long id,
                                       @Valid @RequestBody AdminUpdateRequest request) {
        return ApiResponse.ok(adminManagementService.update(id, request, authUser.id()));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@AuthenticationPrincipal AuthUser authUser,
                                          @PathVariable Long id,
                                          @Valid @RequestBody StatusUpdateRequest request) {
        adminManagementService.updateStatus(id, request.status(), authUser.id());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@AuthenticationPrincipal AuthUser authUser,
                                           @PathVariable Long id,
                                           @Valid @RequestBody AdminPasswordResetRequest request) {
        adminManagementService.resetPassword(id, request, authUser.id());
        return ApiResponse.ok();
    }
}
