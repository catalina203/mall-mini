package com.mallmini.aftersale.controller;

import com.mallmini.aftersale.dto.AdminRefundRequest;
import com.mallmini.aftersale.dto.RejectAfterSaleRequest;
import com.mallmini.aftersale.service.AfterSaleService;
import com.mallmini.aftersale.vo.AfterSaleDetailVO;
import com.mallmini.aftersale.vo.AfterSaleSummaryVO;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/after-sales")
@RequiredArgsConstructor
public class AdminAfterSaleController {

    private final AfterSaleService afterSaleService;

    @PreAuthorize("hasAuthority('AFTER_SALE_READ')")
    @GetMapping
    public ApiResponse<PageResult<AfterSaleSummaryVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String afterSaleNo,
            @RequestParam(required = false) String orderNo) {
        return ApiResponse.ok(afterSaleService.adminList(
                status, afterSaleNo, orderNo, page, pageSize));
    }

    @PreAuthorize("hasAuthority('AFTER_SALE_READ')")
    @GetMapping("/{afterSaleNo}")
    public ApiResponse<AfterSaleDetailVO> detail(@PathVariable String afterSaleNo) {
        return ApiResponse.ok(afterSaleService.adminDetail(afterSaleNo));
    }

    @PreAuthorize("hasAuthority('AFTER_SALE_REVIEW')")
    @PostMapping("/{afterSaleNo}/approve")
    public ApiResponse<AfterSaleDetailVO> approve(@AuthenticationPrincipal AuthUser authUser,
                                                  @PathVariable String afterSaleNo) {
        return ApiResponse.ok(afterSaleService.approve(authUser.id(), afterSaleNo));
    }

    @PreAuthorize("hasAuthority('AFTER_SALE_REVIEW')")
    @PostMapping("/{afterSaleNo}/reject")
    public ApiResponse<AfterSaleDetailVO> reject(@AuthenticationPrincipal AuthUser authUser,
                                                 @PathVariable String afterSaleNo,
                                                 @Valid @RequestBody RejectAfterSaleRequest request) {
        return ApiResponse.ok(afterSaleService.reject(authUser.id(), afterSaleNo, request.reason()));
    }

    @PreAuthorize("hasAuthority('AFTER_SALE_REVIEW')")
    @PostMapping("/{afterSaleNo}/confirm-return")
    public ApiResponse<AfterSaleDetailVO> confirmReturn(@AuthenticationPrincipal AuthUser authUser,
                                                        @PathVariable String afterSaleNo) {
        return ApiResponse.ok(afterSaleService.confirmReturn(authUser.id(), afterSaleNo));
    }

    @PreAuthorize("hasAuthority('AFTER_SALE_REFUND')")
    @PostMapping("/{afterSaleNo}/refund")
    public ApiResponse<AfterSaleDetailVO> refund(@AuthenticationPrincipal AuthUser authUser,
                                                 @PathVariable String afterSaleNo,
                                                 @RequestBody(required = false) AdminRefundRequest request) {
        return ApiResponse.ok(afterSaleService.refund(authUser.id(), afterSaleNo, request));
    }
}
