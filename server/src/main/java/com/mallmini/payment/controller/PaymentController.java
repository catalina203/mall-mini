package com.mallmini.payment.controller;

import com.mallmini.common.api.ApiResponse;
import com.mallmini.payment.service.PaymentService;
import com.mallmini.payment.vo.PaymentConfigVO;
import com.mallmini.payment.vo.PaymentResultVO;
import com.mallmini.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/api/v1/payment/config")
    public ApiResponse<PaymentConfigVO> config() {
        return ApiResponse.ok(paymentService.config());
    }

    @PostMapping("/api/v1/me/orders/{orderNo}/pay")
    public ApiResponse<PaymentResultVO> pay(@AuthenticationPrincipal AuthUser authUser,
                                            @PathVariable String orderNo) {
        return ApiResponse.ok(paymentService.pay(authUser.id(), orderNo));
    }
}
