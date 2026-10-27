package com.mallmini.admin.service;

import com.mallmini.admin.enums.AdminPermission;
import com.mallmini.admin.enums.AdminRole;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class AdminPermissionService {

    private static final Set<AdminPermission> ALL = EnumSet.allOf(AdminPermission.class);

    private static final Set<AdminPermission> OPERATOR = EnumSet.of(
            AdminPermission.DASHBOARD_READ,
            AdminPermission.PRODUCT_READ,
            AdminPermission.PRODUCT_WRITE,
            AdminPermission.CATEGORY_READ,
            AdminPermission.CATEGORY_WRITE,
            AdminPermission.BANNER_READ,
            AdminPermission.BANNER_WRITE,
            AdminPermission.COUPON_READ,
            AdminPermission.COUPON_WRITE,
            AdminPermission.REVIEW_READ,
            AdminPermission.REVIEW_WRITE,
            AdminPermission.ORDER_READ,
            AdminPermission.ORDER_WRITE,
            AdminPermission.ORDER_SHIP,
            AdminPermission.ORDER_CANCEL,
            AdminPermission.AFTER_SALE_READ,
            AdminPermission.AFTER_SALE_REVIEW,
            AdminPermission.AFTER_SALE_REFUND,
            AdminPermission.STOCK_READ,
            AdminPermission.STOCK_ADJUST,
            AdminPermission.USER_READ,
            AdminPermission.USER_STATUS,
            AdminPermission.NOTIFICATION_READ,
            AdminPermission.SUPPORT_READ,
            AdminPermission.SUPPORT_WRITE,
            AdminPermission.EXPORT_READ);

    private static final Set<AdminPermission> CUSTOMER_SERVICE = EnumSet.of(
            AdminPermission.DASHBOARD_READ,
            AdminPermission.ORDER_READ,
            AdminPermission.ORDER_WRITE,
            AdminPermission.AFTER_SALE_READ,
            AdminPermission.AFTER_SALE_REVIEW,
            AdminPermission.USER_READ,
            AdminPermission.NOTIFICATION_READ,
            AdminPermission.SUPPORT_READ,
            AdminPermission.SUPPORT_WRITE,
            AdminPermission.REVIEW_READ,
            AdminPermission.REVIEW_WRITE);

    private static final Set<AdminPermission> WAREHOUSE = EnumSet.of(
            AdminPermission.DASHBOARD_READ,
            AdminPermission.PRODUCT_READ,
            AdminPermission.ORDER_READ,
            AdminPermission.ORDER_SHIP,
            AdminPermission.AFTER_SALE_READ,
            AdminPermission.STOCK_READ,
            AdminPermission.STOCK_ADJUST,
            AdminPermission.NOTIFICATION_READ);

    private AdminPermissionService() {
    }

    public static Set<AdminPermission> permissionsOf(String roleCode) {
        AdminRole role = AdminRole.from(roleCode);
        return switch (role) {
            case SUPER_ADMIN -> ALL;
            case OPERATOR -> OPERATOR;
            case CUSTOMER_SERVICE -> CUSTOMER_SERVICE;
            case WAREHOUSE -> WAREHOUSE;
        };
    }

    public static List<String> permissionCodes(String roleCode) {
        return permissionsOf(roleCode).stream().map(Enum::name).sorted().toList();
    }

    public static boolean hasPermission(String roleCode, AdminPermission permission) {
        return permissionsOf(roleCode).contains(permission);
    }
}
