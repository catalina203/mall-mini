package com.mallmini.admin.controller;

import com.mallmini.admin.service.AdminOperationLogService;
import com.mallmini.admin.vo.AdminOperationLogVO;
import com.mallmini.common.api.ApiResponse;
import com.mallmini.common.api.PageResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/operation-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('AUDIT_READ')")
public class AdminOperationLogController {

    private final AdminOperationLogService adminOperationLogService;

    @GetMapping
    public ApiResponse<PageResult<AdminOperationLogVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType) {
        return ApiResponse.ok(adminOperationLogService.page(adminId, action, targetType, page, pageSize));
    }
}
