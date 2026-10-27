package com.mallmini.product.enums;

import java.util.Locale;

public enum ProductSort {
    NEW,
    SALES,
    PRICE_ASC,
    PRICE_DESC;

    public static ProductSort from(String value) {
        if (value == null || value.isBlank()) {
            return NEW;
        }
        try {
            return ProductSort.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return NEW;
        }
    }

    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
