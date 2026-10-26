package com.mallmini.category.controller;

import com.mallmini.category.service.CategoryService;
import com.mallmini.category.vo.CategoryVO;
import com.mallmini.common.api.ApiResponse;
import com.mallmini.common.api.PageResult;
import com.mallmini.product.service.ProductService;
import com.mallmini.product.vo.ProductSummaryVO;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;

    @GetMapping
    public ApiResponse<List<CategoryVO>> list() {
        return ApiResponse.ok(categoryService.listEnabledTree());
    }

    @GetMapping("/{id}/products")
    public ApiResponse<PageResult<ProductSummaryVO>> products(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) long pageSize,
            @RequestParam(defaultValue = "new") String sort,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal maxPrice) {
        return ApiResponse.ok(productService.listOnSale(
                page, pageSize, id, null, sort, minPrice, maxPrice));
    }
}
