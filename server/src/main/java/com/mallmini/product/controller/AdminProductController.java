package com.mallmini.product.controller;

import com.mallmini.admin.dto.StatusUpdateRequest;
import com.mallmini.common.api.ApiResponse;
import com.mallmini.common.api.PageResult;
import com.mallmini.product.dto.ProductSaveRequest;
import com.mallmini.product.dto.StockUpdateRequest;
import com.mallmini.product.service.AdminProductService;
import com.mallmini.product.vo.AdminProductDetailVO;
import com.mallmini.product.vo.AdminProductSummaryVO;
import com.mallmini.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    @GetMapping("/products")
    public ApiResponse<PageResult<AdminProductSummaryVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(adminProductService.page(page, pageSize, status, categoryId, keyword));
    }

    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    @GetMapping("/products/{id}")
    public ApiResponse<AdminProductDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(adminProductService.detail(id));
    }

    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    @PostMapping("/products")
    public ApiResponse<AdminProductDetailVO> create(@AuthenticationPrincipal AuthUser authUser,
                                                    @Valid @RequestBody ProductSaveRequest request) {
        return ApiResponse.ok(adminProductService.create(request, authUser.id()));
    }

    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    @PutMapping("/products/{id}")
    public ApiResponse<AdminProductDetailVO> update(@AuthenticationPrincipal AuthUser authUser,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody ProductSaveRequest request) {
        return ApiResponse.ok(adminProductService.update(id, request, authUser.id()));
    }

    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    @PatchMapping("/products/{id}/status")
    public ApiResponse<Void> updateStatus(@AuthenticationPrincipal AuthUser authUser,
                                          @PathVariable Long id,
                                          @Valid @RequestBody StatusUpdateRequest request) {
        adminProductService.updateStatus(id, request.status(), authUser.id());
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    @PatchMapping("/skus/{skuId}/stock")
    public ApiResponse<Void> updateStock(@AuthenticationPrincipal AuthUser authUser,
                                         @PathVariable Long skuId,
                                         @Valid @RequestBody StockUpdateRequest request) {
        adminProductService.updateStock(skuId, request.stock(), authUser.id());
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    @DeleteMapping("/products/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id) {
        adminProductService.delete(id, authUser.id());
        return ApiResponse.ok();
    }
}
