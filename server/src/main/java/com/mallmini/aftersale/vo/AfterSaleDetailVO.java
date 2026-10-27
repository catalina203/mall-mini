package com.mallmini.aftersale.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AfterSaleDetailVO(
        String afterSaleNo,
        String orderNo,
        String type,
        String status,
        String reason,
        String description,
        BigDecimal refundAmount,
        String returnCompany,
        String returnTrackingNo,
        String reviewRemark,
        LocalDateTime reviewedAt,
        LocalDateTime returnedAt,
        LocalDateTime refundedAt,
        LocalDateTime createdAt,
        List<AfterSaleItemVO> items,
        List<AfterSaleLogVO> logs,
        RefundInfoVO refund
) {
}
