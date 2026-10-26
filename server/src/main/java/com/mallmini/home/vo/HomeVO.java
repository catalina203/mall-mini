package com.mallmini.home.vo;

import com.mallmini.banner.vo.BannerVO;
import com.mallmini.category.vo.CategoryVO;
import com.mallmini.product.vo.ProductSummaryVO;

import java.util.List;

public record HomeVO(
        List<BannerVO> banners,
        List<CategoryVO> categories,
        List<ProductSummaryVO> recommendedProducts
) {
}
