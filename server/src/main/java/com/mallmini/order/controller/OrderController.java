package com.mallmini.order.controller;

import com.mallmini.common.api.ApiResponse;
import com.mallmini.common.api.PageResult;
import com.mallmini.common.ratelimit.RateLimitService;
import com.mallmini.order.dto.CancelOrderRequest;
import com.mallmini.order.dto.CheckoutPreviewRequest;
import com.mallmini.order.dto.CreateOrderRequest;
import com.mallmini.order.service.OrderService;
import com.mallmini.order.vo.CheckoutVO;
import com.mallmini.order.vo.OrderDetailVO;
import com.mallmini.order.vo.OrderSummaryVO;
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
public class OrderController {

    private static final int ORDER_CREATE_LIMIT_PER_MINUTE = 20;

    private final OrderService orderService;
    private final RateLimitService rateLimitService;

    @PostMapping("/checkout/preview")
    public ApiResponse<CheckoutVO> preview(@AuthenticationPrincipal AuthUser authUser,
                                           @Valid @RequestBody CheckoutPreviewRequest request) {
        return ApiResponse.ok(orderService.preview(authUser.id(), request));
    }

    @PostMapping("/orders")
    public ApiResponse<OrderDetailVO> create(@AuthenticationPrincipal AuthUser authUser,
                                             @Valid @RequestBody CreateOrderRequest request) {
        rateLimitService.check("order:" + authUser.id(), ORDER_CREATE_LIMIT_PER_MINUTE, 60);
        return ApiResponse.ok(orderService.create(authUser.id(), request));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResult<OrderSummaryVO>> list(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize) {
        return ApiResponse.ok(orderService.list(authUser.id(), status, page, pageSize));
    }

    @GetMapping("/orders/{orderNo}")
    public ApiResponse<OrderDetailVO> detail(@AuthenticationPrincipal AuthUser authUser,
                                             @PathVariable String orderNo) {
        return ApiResponse.ok(orderService.detail(authUser.id(), orderNo));
    }

    @PostMapping("/orders/{orderNo}/cancel")
    public ApiResponse<OrderDetailVO> cancel(@AuthenticationPrincipal AuthUser authUser,
                                             @PathVariable String orderNo,
                                             @RequestBody(required = false) CancelOrderRequest request) {
        String reason = request == null ? null : request.reason();
        return ApiResponse.ok(orderService.cancel(authUser.id(), orderNo, reason));
    }

    @PostMapping("/orders/{orderNo}/confirm-receipt")
    public ApiResponse<OrderDetailVO> confirmReceipt(@AuthenticationPrincipal AuthUser authUser,
                                                     @PathVariable String orderNo) {
        return ApiResponse.ok(orderService.confirmReceipt(authUser.id(), orderNo));
    }
}
