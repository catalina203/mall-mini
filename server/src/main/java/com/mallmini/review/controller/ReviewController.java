package com.mallmini.review.controller;

import com.mallmini.common.api.ApiResponse;
import com.mallmini.common.api.PageResult;
import com.mallmini.review.dto.CreateReviewRequest;
import com.mallmini.review.service.ReviewService;
import com.mallmini.review.vo.ReviewSummaryVO;
import com.mallmini.review.vo.ReviewVO;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/me/orders/{orderNo}/reviews")
    public ApiResponse<ReviewVO> create(@AuthenticationPrincipal AuthUser authUser,
                                        @PathVariable String orderNo,
                                        @Valid @RequestBody CreateReviewRequest request) {
        return ApiResponse.ok(reviewService.create(authUser.id(), orderNo, request));
    }

    @GetMapping("/products/{productId}/reviews")
    public ApiResponse<PageResult<ReviewVO>> list(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize) {
        return ApiResponse.ok(reviewService.listForProduct(productId, page, pageSize));
    }

    @GetMapping("/products/{productId}/reviews/summary")
    public ApiResponse<ReviewSummaryVO> summary(@PathVariable Long productId) {
        return ApiResponse.ok(reviewService.summary(productId));
    }
}
