package com.mallmini.support.controller;

import com.mallmini.common.api.ApiResponse;
import com.mallmini.common.api.PageResult;
import com.mallmini.security.AuthUser;
import com.mallmini.support.dto.TicketReplyRequest;
import com.mallmini.support.service.SupportTicketService;
import com.mallmini.support.vo.SupportTicketVO;
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
@RequestMapping("/api/v1/admin/support-tickets")
@RequiredArgsConstructor
public class AdminSupportTicketController {

    private final SupportTicketService supportTicketService;

    @PreAuthorize("hasAuthority('SUPPORT_READ')")
    @GetMapping
    public ApiResponse<PageResult<SupportTicketVO>> list(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(supportTicketService.adminList(status, type, keyword, page, pageSize));
    }

    @PreAuthorize("hasAuthority('SUPPORT_WRITE')")
    @PostMapping("/{ticketNo}/reply")
    public ApiResponse<SupportTicketVO> reply(@AuthenticationPrincipal AuthUser authUser,
                                              @PathVariable String ticketNo,
                                              @Valid @RequestBody TicketReplyRequest request) {
        return ApiResponse.ok(supportTicketService.reply(authUser.id(), ticketNo, request.reply()));
    }

    @PreAuthorize("hasAuthority('SUPPORT_WRITE')")
    @PostMapping("/{ticketNo}/close")
    public ApiResponse<Void> close(@AuthenticationPrincipal AuthUser authUser,
                                   @PathVariable String ticketNo) {
        supportTicketService.closeForAdmin(authUser.id(), ticketNo);
        return ApiResponse.ok();
    }
}
