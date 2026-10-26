package com.mallmini.cart.controller;

import com.mallmini.cart.dto.AddCartItemRequest;
import com.mallmini.cart.dto.CartSelectionRequest;
import com.mallmini.cart.dto.UpdateCartItemRequest;
import com.mallmini.cart.service.CartService;
import com.mallmini.cart.vo.CartVO;
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

@RestController
@RequestMapping("/api/v1/me/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<CartVO> list(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(cartService.list(authUser.id()));
    }

    @PostMapping
    public ApiResponse<CartVO> add(@AuthenticationPrincipal AuthUser authUser,
                                   @Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.ok(cartService.add(authUser.id(), request));
    }

    @PutMapping("/selection")
    public ApiResponse<CartVO> select(@AuthenticationPrincipal AuthUser authUser,
                                      @Valid @RequestBody CartSelectionRequest request) {
        return ApiResponse.ok(cartService.select(authUser.id(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CartVO> update(@AuthenticationPrincipal AuthUser authUser,
                                      @PathVariable Long id,
                                      @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.ok(cartService.update(authUser.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<CartVO> delete(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id) {
        return ApiResponse.ok(cartService.delete(authUser.id(), id));
    }

    @DeleteMapping
    public ApiResponse<CartVO> clear(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(cartService.clear(authUser.id()));
    }
}
