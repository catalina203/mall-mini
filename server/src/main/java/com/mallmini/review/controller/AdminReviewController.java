package com.mallmini.review.controller;

import com.mallmini.common.api.ApiResponse;
import com.mallmini.common.api.PageResult;
import com.mallmini.review.dto.ReviewReplyRequest;
import com.mallmini.review.service.ReviewService;
import com.mallmini.review.vo.ReviewVO;
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
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    @PreAuthorize("hasAuthority('REVIEW_READ')")
    @GetMapping
    public ApiResponse<PageResult<ReviewVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long productId) {
        return ApiResponse.ok(reviewService.adminList(status, productId, page, pageSize));
    }

    @PreAuthorize("hasAuthority('REVIEW_WRITE')")
    @PostMapping("/{id}/approve")
    public ApiResponse<ReviewVO> approve(@AuthenticationPrincipal AuthUser authUser,
                                         @PathVariable Long id) {
        return ApiResponse.ok(reviewService.approve(authUser.id(), id));
    }

    @PreAuthorize("hasAuthority('REVIEW_WRITE')")
    @PostMapping("/{id}/reject")
    public ApiResponse<ReviewVO> reject(@AuthenticationPrincipal AuthUser authUser,
                                        @PathVariable Long id) {
        return ApiResponse.ok(reviewService.reject(authUser.id(), id));
    }

    @PreAuthorize("hasAuthority('REVIEW_WRITE')")
    @PostMapping("/{id}/reply")
    public ApiResponse<ReviewVO> reply(@AuthenticationPrincipal AuthUser authUser,
                                       @PathVariable Long id,
                                       @Valid @RequestBody ReviewReplyRequest request) {
        return ApiResponse.ok(reviewService.reply(authUser.id(), id, request.reply()));
    }
}
