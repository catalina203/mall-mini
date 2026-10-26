package com.mallmini.banner.controller;

import com.mallmini.admin.dto.StatusUpdateRequest;
import com.mallmini.banner.dto.BannerSaveRequest;
import com.mallmini.banner.service.AdminBannerService;
import com.mallmini.banner.vo.AdminBannerVO;
import com.mallmini.common.api.ApiResponse;
import com.mallmini.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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

@RestController
@RequestMapping("/api/v1/admin/banners")
@RequiredArgsConstructor
public class AdminBannerController {

    private final AdminBannerService adminBannerService;

    @GetMapping
    public ApiResponse<List<AdminBannerVO>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(adminBannerService.list(status));
    }

    @PostMapping
    public ApiResponse<AdminBannerVO> create(@AuthenticationPrincipal AuthUser authUser,
                                             @Valid @RequestBody BannerSaveRequest request) {
        return ApiResponse.ok(adminBannerService.create(request, authUser.id()));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminBannerVO> update(@AuthenticationPrincipal AuthUser authUser,
                                             @PathVariable Long id,
                                             @Valid @RequestBody BannerSaveRequest request) {
        return ApiResponse.ok(adminBannerService.update(id, request, authUser.id()));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@AuthenticationPrincipal AuthUser authUser,
                                          @PathVariable Long id,
                                          @Valid @RequestBody StatusUpdateRequest request) {
        adminBannerService.updateStatus(id, request.status(), authUser.id());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id) {
        adminBannerService.delete(id, authUser.id());
        return ApiResponse.ok();
    }
}
