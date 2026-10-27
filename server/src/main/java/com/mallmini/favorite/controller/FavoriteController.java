package com.mallmini.favorite.controller;

import com.mallmini.common.api.ApiResponse;
import com.mallmini.common.api.PageResult;
import com.mallmini.favorite.service.FavoriteService;
import com.mallmini.favorite.vo.FavoriteProductVO;
import com.mallmini.security.AuthUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/favorites/{productId}")
    public ApiResponse<Void> add(@AuthenticationPrincipal AuthUser authUser,
                                 @PathVariable Long productId) {
        favoriteService.addFavorite(authUser.id(), productId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/favorites/{productId}")
    public ApiResponse<Void> remove(@AuthenticationPrincipal AuthUser authUser,
                                    @PathVariable Long productId) {
        favoriteService.removeFavorite(authUser.id(), productId);
        return ApiResponse.ok();
    }

    @GetMapping("/favorites/{productId}/status")
    public ApiResponse<Boolean> status(@AuthenticationPrincipal AuthUser authUser,
                                       @PathVariable Long productId) {
        return ApiResponse.ok(favoriteService.isFavorite(authUser.id(), productId));
    }

    @GetMapping("/favorites")
    public ApiResponse<PageResult<FavoriteProductVO>> favorites(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize) {
        return ApiResponse.ok(favoriteService.favorites(authUser.id(), page, pageSize));
    }

    @PostMapping("/browse-history/{productId}")
    public ApiResponse<Void> recordHistory(@AuthenticationPrincipal AuthUser authUser,
                                           @PathVariable Long productId) {
        favoriteService.recordHistory(authUser.id(), productId);
        return ApiResponse.ok();
    }

    @GetMapping("/browse-history")
    public ApiResponse<PageResult<FavoriteProductVO>> history(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize) {
        return ApiResponse.ok(favoriteService.history(authUser.id(), page, pageSize));
    }

    @DeleteMapping("/browse-history")
    public ApiResponse<Void> clearHistory(@AuthenticationPrincipal AuthUser authUser) {
        favoriteService.clearHistory(authUser.id());
        return ApiResponse.ok();
    }
}
