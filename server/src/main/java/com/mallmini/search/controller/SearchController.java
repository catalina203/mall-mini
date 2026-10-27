package com.mallmini.search.controller;

import com.mallmini.common.api.ApiResponse;
import com.mallmini.search.dto.SearchKeywordRequest;
import com.mallmini.search.service.SearchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/keywords")
    public ApiResponse<Void> record(@Valid @RequestBody SearchKeywordRequest request) {
        searchService.record(request.keyword());
        return ApiResponse.ok();
    }

    @GetMapping("/hot")
    public ApiResponse<List<String>> hot(
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int limit) {
        return ApiResponse.ok(searchService.hot(limit));
    }
}
