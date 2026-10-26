package com.mallmini.home.controller;

import com.mallmini.common.api.ApiResponse;
import com.mallmini.home.service.HomeService;
import com.mallmini.home.vo.HomeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/home")
    public ApiResponse<HomeVO> home() {
        return ApiResponse.ok(homeService.getHome());
    }
}
