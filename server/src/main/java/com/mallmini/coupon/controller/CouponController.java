package com.mallmini.coupon.controller;

import com.mallmini.common.api.ApiResponse;
import com.mallmini.coupon.service.CouponService;
import com.mallmini.coupon.vo.CouponTemplateVO;
import com.mallmini.coupon.vo.UserCouponVO;
import com.mallmini.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    public ApiResponse<List<UserCouponVO>> myCoupons(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(couponService.myCoupons(authUser.id(), status));
    }

    @GetMapping("/claimable")
    public ApiResponse<List<CouponTemplateVO>> claimable(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(couponService.claimable(authUser.id()));
    }

    @PostMapping("/{templateId}/claim")
    public ApiResponse<UserCouponVO> claim(@AuthenticationPrincipal AuthUser authUser,
                                           @PathVariable Long templateId) {
        return ApiResponse.ok(couponService.claim(authUser.id(), templateId));
    }
}
