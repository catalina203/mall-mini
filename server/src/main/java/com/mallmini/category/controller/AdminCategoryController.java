package com.mallmini.category.controller;

import com.mallmini.admin.dto.StatusUpdateRequest;
import com.mallmini.category.dto.CategorySaveRequest;
import com.mallmini.category.service.AdminCategoryService;
import com.mallmini.category.vo.AdminCategoryVO;
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

@Validated
@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    @PreAuthorize("hasAuthority('CATEGORY_READ')")
    @GetMapping
    public ApiResponse<PageResult<AdminCategoryVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(adminCategoryService.page(page, pageSize, status, keyword));
    }

    @PreAuthorize("hasAuthority('CATEGORY_READ')")
    @GetMapping("/options")
    public ApiResponse<List<AdminCategoryVO>> options() {
        return ApiResponse.ok(adminCategoryService.options());
    }

    @PreAuthorize("hasAuthority('CATEGORY_WRITE')")
    @PostMapping
    public ApiResponse<AdminCategoryVO> create(@AuthenticationPrincipal AuthUser authUser,
                                               @Valid @RequestBody CategorySaveRequest request) {
        return ApiResponse.ok(adminCategoryService.create(request, authUser.id()));
    }

    @PreAuthorize("hasAuthority('CATEGORY_WRITE')")
    @PutMapping("/{id}")
    public ApiResponse<AdminCategoryVO> update(@AuthenticationPrincipal AuthUser authUser,
                                               @PathVariable Long id,
                                               @Valid @RequestBody CategorySaveRequest request) {
        return ApiResponse.ok(adminCategoryService.update(id, request, authUser.id()));
    }

    @PreAuthorize("hasAuthority('CATEGORY_WRITE')")
    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@AuthenticationPrincipal AuthUser authUser,
                                          @PathVariable Long id,
                                          @Valid @RequestBody StatusUpdateRequest request) {
        adminCategoryService.updateStatus(id, request.status(), authUser.id());
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('CATEGORY_WRITE')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id) {
        adminCategoryService.delete(id, authUser.id());
        return ApiResponse.ok();
    }
}
