package com.mallmini.support.controller;

import com.mallmini.common.api.ApiResponse;
import com.mallmini.common.api.PageResult;
import com.mallmini.security.AuthUser;
import com.mallmini.support.dto.CreateTicketRequest;
import com.mallmini.support.service.SupportTicketService;
import com.mallmini.support.vo.SupportTicketVO;
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
@RequestMapping("/api/v1/me/support-tickets")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    @PostMapping
    public ApiResponse<SupportTicketVO> create(@AuthenticationPrincipal AuthUser authUser,
                                               @Valid @RequestBody CreateTicketRequest request) {
        return ApiResponse.ok(supportTicketService.create(authUser.id(), request));
    }

    @GetMapping
    public ApiResponse<PageResult<SupportTicketVO>> list(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize) {
        return ApiResponse.ok(supportTicketService.listForUser(authUser.id(), page, pageSize));
    }

    @GetMapping("/{ticketNo}")
    public ApiResponse<SupportTicketVO> detail(@AuthenticationPrincipal AuthUser authUser,
                                               @PathVariable String ticketNo) {
        return ApiResponse.ok(supportTicketService.detailForUser(authUser.id(), ticketNo));
    }

    @PostMapping("/{ticketNo}/close")
    public ApiResponse<Void> close(@AuthenticationPrincipal AuthUser authUser,
                                   @PathVariable String ticketNo) {
        supportTicketService.closeForUser(authUser.id(), ticketNo);
        return ApiResponse.ok();
    }
}
