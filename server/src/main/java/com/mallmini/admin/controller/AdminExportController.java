package com.mallmini.admin.controller;

import com.mallmini.admin.service.ExportService;
import com.mallmini.security.AuthUser;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/admin/exports")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('EXPORT_READ')")
public class AdminExportController {

    private final ExportService exportService;

    @GetMapping("/orders")
    public void orders(@AuthenticationPrincipal AuthUser authUser,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String orderNo,
                       @RequestParam(required = false) Long userId,
                       HttpServletResponse response) throws IOException {
        exportService.exportOrders(status, orderNo, userId, authUser.id(), response);
    }

    @GetMapping("/users")
    public void users(@AuthenticationPrincipal AuthUser authUser,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String status,
                      HttpServletResponse response) throws IOException {
        exportService.exportUsers(keyword, status, authUser.id(), response);
    }

    @GetMapping("/stock-logs")
    public void stockLogs(@AuthenticationPrincipal AuthUser authUser,
                          @RequestParam(required = false) Long skuId,
                          @RequestParam(required = false) String bizNo,
                          @RequestParam(required = false) String changeType,
                          HttpServletResponse response) throws IOException {
        exportService.exportStockLogs(skuId, bizNo, changeType, authUser.id(), response);
    }
}
