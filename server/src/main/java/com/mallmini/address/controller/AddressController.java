package com.mallmini.address.controller;

import com.mallmini.address.dto.AddressRequest;
import com.mallmini.address.service.AddressService;
import com.mallmini.address.vo.AddressVO;
import com.mallmini.common.api.ApiResponse;
import com.mallmini.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ApiResponse<List<AddressVO>> list(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(addressService.list(authUser.id()));
    }

    @PostMapping
    public ApiResponse<AddressVO> create(@AuthenticationPrincipal AuthUser authUser,
                                         @Valid @RequestBody AddressRequest request) {
        return ApiResponse.ok(addressService.create(authUser.id(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AddressVO> update(@AuthenticationPrincipal AuthUser authUser,
                                         @PathVariable Long id,
                                         @Valid @RequestBody AddressRequest request) {
        return ApiResponse.ok(addressService.update(authUser.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id) {
        addressService.delete(authUser.id(), id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/default")
    public ApiResponse<Void> setDefault(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id) {
        addressService.setDefault(authUser.id(), id);
        return ApiResponse.ok();
    }
}
