package com.mallmini.common.util;

public final class MaskUtils {

    private MaskUtils() {
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        if (phone.length() >= 11) {
            return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
        }
        if (phone.length() >= 7) {
            return phone.substring(0, 2) + "****" + phone.substring(phone.length() - 2);
        }
        return "****";
    }
}
