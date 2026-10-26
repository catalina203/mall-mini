package com.mallmini.banner.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallmini.admin.service.AdminOperationLogger;
import com.mallmini.banner.dto.BannerSaveRequest;
import com.mallmini.banner.entity.Banner;
import com.mallmini.banner.enums.BannerLinkType;
import com.mallmini.banner.enums.BannerStatus;
import com.mallmini.banner.mapper.BannerMapper;
import com.mallmini.banner.vo.AdminBannerVO;
import com.mallmini.category.mapper.CategoryMapper;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminBannerService {

    private final BannerMapper bannerMapper;
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final AdminOperationLogger operationLogger;

    public List<AdminBannerVO> list(String status) {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<Banner>()
                .orderByAsc(Banner::getSortOrder)
                .orderByAsc(Banner::getId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(Banner::getStatus, parseStatus(status).name());
        }
        return bannerMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Transactional
    public AdminBannerVO create(BannerSaveRequest request, Long adminId) {
        Banner banner = new Banner();
        apply(banner, request);
        banner.setStatus(parseStatus(request.status() == null
                ? BannerStatus.ENABLED.name() : request.status()).name());
        bannerMapper.insert(banner);
        operationLogger.log(adminId, "BANNER_CREATE", "BANNER", String.valueOf(banner.getId()), banner.getTitle());
        return toVO(banner);
    }

    @Transactional
    public AdminBannerVO update(Long id, BannerSaveRequest request, Long adminId) {
        Banner banner = require(id);
        apply(banner, request);
        banner.setStatus(parseStatus(request.status() == null
                ? banner.getStatus() : request.status()).name());
        bannerMapper.updateById(banner);
        operationLogger.log(adminId, "BANNER_UPDATE", "BANNER", String.valueOf(id), banner.getTitle());
        return toVO(banner);
    }

    @Transactional
    public void updateStatus(Long id, String status, Long adminId) {
        Banner banner = require(id);
        banner.setStatus(parseStatus(status).name());
        bannerMapper.updateById(banner);
        operationLogger.log(adminId, "BANNER_STATUS", "BANNER", String.valueOf(id), banner.getStatus());
    }

    @Transactional
    public void delete(Long id, Long adminId) {
        Banner banner = require(id);
        bannerMapper.deleteById(id);
        operationLogger.log(adminId, "BANNER_DELETE", "BANNER", String.valueOf(id), banner.getTitle());
    }

    private void apply(Banner banner, BannerSaveRequest request) {
        BannerLinkType linkType = parseLinkType(request.linkType());
        String target = request.linkTarget() == null ? "" : request.linkTarget().trim();
        if (linkType == BannerLinkType.PRODUCT) {
            Long productId = parseId(target, "商品");
            if (productMapper.selectById(productId) == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "跳转商品不存在");
            }
            target = String.valueOf(productId);
        } else if (linkType == BannerLinkType.CATEGORY) {
            Long categoryId = parseId(target, "分类");
            if (categoryMapper.selectById(categoryId) == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "跳转分类不存在");
            }
            target = String.valueOf(categoryId);
        } else {
            target = "";
        }

        banner.setTitle(request.title() == null ? "" : request.title().trim());
        banner.setImageUrl(request.imageUrl().trim());
        banner.setLinkType(linkType.name());
        banner.setLinkTarget(target);
        banner.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    private Long parseId(String value, String label) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "跳转" + label + " ID 不合法");
        }
    }

    private Banner require(Long id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "轮播图不存在");
        }
        return banner;
    }

    private BannerStatus parseStatus(String status) {
        try {
            return BannerStatus.valueOf(status.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "轮播图状态不合法");
        }
    }

    private BannerLinkType parseLinkType(String type) {
        try {
            return BannerLinkType.valueOf(type.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "跳转类型不合法");
        }
    }

    private AdminBannerVO toVO(Banner banner) {
        return new AdminBannerVO(
                banner.getId(),
                banner.getTitle(),
                banner.getImageUrl(),
                banner.getLinkType(),
                banner.getLinkTarget(),
                banner.getSortOrder(),
                banner.getStatus(),
                banner.getCreatedAt());
    }
}
