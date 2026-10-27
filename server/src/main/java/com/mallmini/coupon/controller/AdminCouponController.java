package com.mallmini.coupon.controller;

import com.mallmini.admin.dto.StatusUpdateRequest;
import com.mallmini.common.api.ApiResponse;
import com.mallmini.coupon.dto.CouponTemplateSaveRequest;
import com.mallmini.coupon.service.CouponService;
import com.mallmini.coupon.vo.CouponTemplateVO;
import com.mallmini.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    @PreAuthorize("hasAuthority('COUPON_READ')")
    @GetMapping
    public ApiResponse<List<CouponTemplateVO>> list() {
        return ApiResponse.ok(couponService.templates());
    }

    @PreAuthorize("hasAuthority('COUPON_WRITE')")
    @PostMapping
    public ApiResponse<CouponTemplateVO> create(@AuthenticationPrincipal AuthUser authUser,
                                                @Valid @RequestBody CouponTemplateSaveRequest request) {
        return ApiResponse.ok(couponService.create(request, authUser.id()));
    }

    @PreAuthorize("hasAuthority('COUPON_WRITE')")
    @PutMapping("/{id}")
    public ApiResponse<CouponTemplateVO> update(@AuthenticationPrincipal AuthUser authUser,
                                                @PathVariable Long id,
                                                @Valid @RequestBody CouponTemplateSaveRequest request) {
        return ApiResponse.ok(couponService.update(id, request, authUser.id()));
    }

    @PreAuthorize("hasAuthority('COUPON_WRITE')")
    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@AuthenticationPrincipal AuthUser authUser,
                                          @PathVariable Long id,
                                          @Valid @RequestBody StatusUpdateRequest request) {
        couponService.updateStatus(id, request.status(), authUser.id());
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('COUPON_WRITE')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser authUser,
                                    @PathVariable Long id) {
        couponService.delete(id, authUser.id());
        return ApiResponse.ok();
    }
}
