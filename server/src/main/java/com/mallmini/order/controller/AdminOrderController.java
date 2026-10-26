package com.mallmini.order.controller;

import com.mallmini.common.api.ApiResponse;
import com.mallmini.common.api.PageResult;
import com.mallmini.order.dto.CancelOrderRequest;
import com.mallmini.order.dto.ShipOrderRequest;
import com.mallmini.order.service.OrderService;
import com.mallmini.order.vo.AdminOrderSummaryVO;
import com.mallmini.order.vo.OrderDetailVO;
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
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<PageResult<AdminOrderSummaryVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId) {
        return ApiResponse.ok(orderService.adminList(status, orderNo, userId, page, pageSize));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<OrderDetailVO> detail(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.adminDetail(orderNo));
    }

    @PostMapping("/{orderNo}/ship")
    public ApiResponse<OrderDetailVO> ship(@AuthenticationPrincipal AuthUser authUser,
                                           @PathVariable String orderNo,
                                           @Valid @RequestBody ShipOrderRequest request) {
        return ApiResponse.ok(orderService.ship(
                orderNo, request.logisticsCompany(), request.logisticsNo(), authUser.id()));
    }

    @PostMapping("/{orderNo}/cancel")
    public ApiResponse<OrderDetailVO> cancel(@AuthenticationPrincipal AuthUser authUser,
                                             @PathVariable String orderNo,
                                             @RequestBody(required = false) CancelOrderRequest request) {
        String reason = request == null ? null : request.reason();
        return ApiResponse.ok(orderService.adminCancel(orderNo, reason, authUser.id()));
    }
}
