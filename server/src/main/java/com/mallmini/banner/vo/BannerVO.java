package com.mallmini.banner.vo;

public record BannerVO(
        Long id,
        String title,
        String imageUrl,
        String linkType,
        String linkTarget
) {
}
