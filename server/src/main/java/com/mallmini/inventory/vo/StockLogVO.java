package com.mallmini.inventory.vo;

import java.time.LocalDateTime;

public record StockLogVO(
        Long id,
        Long skuId,
        Long productId,
        String skuCode,
        String productName,
        String changeType,
        int quantityDelta,
        int stockBefore,
        int stockAfter,
        String bizType,
        String bizNo,
        String operatorType,
        Long operatorId,
        String remark,
        LocalDateTime createdAt
) {
}
