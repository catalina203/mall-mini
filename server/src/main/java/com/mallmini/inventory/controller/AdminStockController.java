package com.mallmini.inventory.controller;

import com.mallmini.common.api.ApiResponse;
import com.mallmini.common.api.PageResult;
import com.mallmini.inventory.dto.StockAdjustRequest;
import com.mallmini.inventory.service.StockService;
import com.mallmini.inventory.vo.StockAlertVO;
import com.mallmini.inventory.vo.StockLogVO;
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

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/admin/stock")
@RequiredArgsConstructor
public class AdminStockController {

    private final StockService stockService;

    @PreAuthorize("hasAuthority('STOCK_READ')")
    @GetMapping("/logs")
    public ApiResponse<PageResult<StockLogVO>> logs(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) String bizNo,
            @RequestParam(required = false) String changeType) {
        return ApiResponse.ok(stockService.page(skuId, bizNo, changeType, page, pageSize));
    }

    @PreAuthorize("hasAuthority('STOCK_READ')")
    @GetMapping("/alerts")
    public ApiResponse<List<StockAlertVO>> alerts(
            @RequestParam(required = false) @Min(0) @Max(99999999) Integer threshold) {
        return ApiResponse.ok(stockService.alerts(threshold));
    }

    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    @PostMapping("/skus/{skuId}/adjust")
    public ApiResponse<Void> adjust(@AuthenticationPrincipal AuthUser authUser,
                                    @PathVariable Long skuId,
                                    @Valid @RequestBody StockAdjustRequest request) {
        stockService.adjust(skuId, request.stock(), authUser.id(), request.remark());
        return ApiResponse.ok();
    }
}
