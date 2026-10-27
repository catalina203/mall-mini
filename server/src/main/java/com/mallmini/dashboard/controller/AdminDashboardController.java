package com.mallmini.dashboard.controller;

import com.mallmini.common.api.ApiResponse;
import com.mallmini.dashboard.service.DashboardService;
import com.mallmini.dashboard.vo.DashboardSummaryVO;
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
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @PreAuthorize("hasAuthority('DASHBOARD_READ')")
    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryVO> summary(
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        return ApiResponse.ok(dashboardService.summary(days));
    }
}
