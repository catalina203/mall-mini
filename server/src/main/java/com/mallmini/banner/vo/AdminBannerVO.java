package com.mallmini.banner.vo;

import java.time.LocalDateTime;

public record AdminBannerVO(
        Long id,
        String title,
        String imageUrl,
        String linkType,
        String linkTarget,
        Integer sortOrder,
        String status,
        LocalDateTime createdAt
) {
}
