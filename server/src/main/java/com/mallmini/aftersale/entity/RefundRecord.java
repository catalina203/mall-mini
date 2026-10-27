package com.mallmini.aftersale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("refund_record")
public class RefundRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String refundNo;

    private Long afterSaleId;

    private Long orderId;

    private String orderNo;

    private String channel;

    private BigDecimal amount;

    private String status;

    private String transactionNo;

    private LocalDateTime requestedAt;

    private LocalDateTime successAt;

    private String failedReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
