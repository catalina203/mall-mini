package com.mallmini.notification.controller;

import com.mallmini.common.api.ApiResponse;
import com.mallmini.common.api.PageResult;
import com.mallmini.notification.service.NotificationService;
import com.mallmini.notification.vo.AdminNotificationVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/notification-logs")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;

    @PreAuthorize("hasAuthority('NOTIFICATION_READ')")
    @GetMapping
    public ApiResponse<PageResult<AdminNotificationVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String templateCode) {
        return ApiResponse.ok(notificationService.adminPage(status, templateCode, page, pageSize));
    }
}
