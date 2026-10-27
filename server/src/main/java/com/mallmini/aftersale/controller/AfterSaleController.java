package com.mallmini.aftersale.controller;

import com.mallmini.aftersale.dto.CreateAfterSaleRequest;
import com.mallmini.aftersale.dto.ReturnShipmentRequest;
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
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    @PostMapping("/orders/{orderNo}/after-sales")
    public ApiResponse<AfterSaleDetailVO> create(@AuthenticationPrincipal AuthUser authUser,
                                                 @PathVariable String orderNo,
                                                 @Valid @RequestBody CreateAfterSaleRequest request) {
        return ApiResponse.ok(afterSaleService.create(authUser.id(), orderNo, request));
    }

    @GetMapping("/after-sales")
    public ApiResponse<PageResult<AfterSaleSummaryVO>> page(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(afterSaleService.listForUser(authUser.id(), status, page, pageSize));
    }

    @GetMapping("/after-sales/{afterSaleNo}")
    public ApiResponse<AfterSaleDetailVO> detail(@AuthenticationPrincipal AuthUser authUser,
                                                 @PathVariable String afterSaleNo) {
        return ApiResponse.ok(afterSaleService.detailForUser(authUser.id(), afterSaleNo));
    }

    @PostMapping("/after-sales/{afterSaleNo}/cancel")
    public ApiResponse<AfterSaleDetailVO> cancel(@AuthenticationPrincipal AuthUser authUser,
                                                 @PathVariable String afterSaleNo) {
        return ApiResponse.ok(afterSaleService.cancel(authUser.id(), afterSaleNo));
    }

    @PostMapping("/after-sales/{afterSaleNo}/return-shipment")
    public ApiResponse<AfterSaleDetailVO> submitReturn(@AuthenticationPrincipal AuthUser authUser,
                                                       @PathVariable String afterSaleNo,
                                                       @Valid @RequestBody ReturnShipmentRequest request) {
        return ApiResponse.ok(afterSaleService.submitReturn(
                authUser.id(), afterSaleNo, request.company(), request.trackingNo()));
    }
}
