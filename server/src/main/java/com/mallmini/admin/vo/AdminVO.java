package com.mallmini.admin.vo;

public record AdminVO(
        Long id,
        String username,
        String displayName,
        String role,
        String status
) {
}
