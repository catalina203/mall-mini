package com.mallmini.admin.enums;

import java.util.List;
import java.util.Locale;

public enum AdminRole {
    SUPER_ADMIN("超级管理员"),
    OPERATOR("运营"),
    CUSTOMER_SERVICE("客服"),
    WAREHOUSE("仓储");

    private final String label;

    AdminRole(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static AdminRole from(String value) {
        if (value == null || value.isBlank()) {
            return OPERATOR;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("ADMIN".equals(normalized)) {
            return SUPER_ADMIN;
        }
        try {
            return AdminRole.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return OPERATOR;
        }
    }

    public static List<String> options() {
        return List.of(SUPER_ADMIN.name(), OPERATOR.name(), CUSTOMER_SERVICE.name(), WAREHOUSE.name());
    }
}
