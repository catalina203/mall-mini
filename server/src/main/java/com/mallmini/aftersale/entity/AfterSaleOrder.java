package com.mallmini.aftersale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("after_sale_order")
public class AfterSaleOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String afterSaleNo;

    private Long orderId;

    private String orderNo;

    private Long userId;

    private String type;

    private String status;

    private String reason;

    private String description;

    private BigDecimal refundAmount;

    private String returnCompany;

    private String returnTrackingNo;

    private String reviewRemark;

    private Long reviewedBy;

    private LocalDateTime reviewedAt;

    private LocalDateTime returnedAt;

    private LocalDateTime refundedAt;

    private LocalDateTime closedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
