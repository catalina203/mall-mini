package com.mallmini.home.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallmini.banner.entity.Banner;
import com.mallmini.banner.enums.BannerStatus;
import com.mallmini.banner.mapper.BannerMapper;
import com.mallmini.banner.vo.BannerVO;
import com.mallmini.category.service.CategoryService;
import com.mallmini.home.vo.HomeVO;
import com.mallmini.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeService {

    private static final int MAX_BANNERS = 10;
    private static final int RECOMMENDED_LIMIT = 8;

    private final BannerMapper bannerMapper;
    private final CategoryService categoryService;
    private final ProductService productService;

    public HomeVO getHome() {
        List<Banner> banners = bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getStatus, BannerStatus.ENABLED.name())
                .orderByAsc(Banner::getSortOrder)
                .orderByAsc(Banner::getId));
        List<BannerVO> bannerVOs = banners.stream()
                .limit(MAX_BANNERS)
                .map(banner -> new BannerVO(
                        banner.getId(),
                        banner.getTitle(),
                        banner.getImageUrl(),
                        banner.getLinkType(),
                        banner.getLinkTarget()))
                .toList();

        return new HomeVO(
                bannerVOs,
                categoryService.listEnabledTree(),
                productService.recommended(RECOMMENDED_LIMIT));
    }
}
