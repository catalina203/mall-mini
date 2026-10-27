package com.mallmini.aftersale.vo;

import java.time.LocalDateTime;

public record AfterSaleLogVO(
        String fromStatus,
        String toStatus,
        String operatorType,
        String remark,
        LocalDateTime createdAt
) {
}
