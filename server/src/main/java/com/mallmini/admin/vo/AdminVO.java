package com.mallmini.admin.vo;

import java.util.List;

public record AdminVO(
        Long id,
        String username,
        String displayName,
        String role,
        String status,
        List<String> permissions
) {
}
