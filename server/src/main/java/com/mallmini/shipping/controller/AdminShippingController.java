package com.mallmini.shipping.controller;

import com.mallmini.admin.dto.StatusUpdateRequest;
import com.mallmini.common.api.ApiResponse;
import com.mallmini.security.AuthUser;
import com.mallmini.shipping.dto.ShippingTemplateSaveRequest;
import com.mallmini.shipping.service.ShippingService;
import com.mallmini.shipping.vo.ShippingOptionVO;
import com.mallmini.shipping.vo.ShippingTemplateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/shipping-templates")
@RequiredArgsConstructor
public class AdminShippingController {

    private final ShippingService shippingService;

    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    @GetMapping
    public ApiResponse<List<ShippingTemplateVO>> list() {
        return ApiResponse.ok(shippingService.list());
    }

    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    @GetMapping("/options")
    public ApiResponse<List<ShippingOptionVO>> options() {
        return ApiResponse.ok(shippingService.options());
    }

    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    @PostMapping
    public ApiResponse<ShippingTemplateVO> create(@AuthenticationPrincipal AuthUser authUser,
                                                  @Valid @RequestBody ShippingTemplateSaveRequest request) {
        return ApiResponse.ok(shippingService.create(request, authUser.id()));
    }

    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    @PutMapping("/{id}")
    public ApiResponse<ShippingTemplateVO> update(@AuthenticationPrincipal AuthUser authUser,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody ShippingTemplateSaveRequest request) {
        return ApiResponse.ok(shippingService.update(id, request, authUser.id()));
    }

    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@AuthenticationPrincipal AuthUser authUser,
                                          @PathVariable Long id,
                                          @Valid @RequestBody StatusUpdateRequest request) {
        shippingService.updateStatus(id, request.status(), authUser.id());
        return ApiResponse.ok();
    }

    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser authUser,
                                    @PathVariable Long id) {
        shippingService.delete(id, authUser.id());
        return ApiResponse.ok();
    }
}
