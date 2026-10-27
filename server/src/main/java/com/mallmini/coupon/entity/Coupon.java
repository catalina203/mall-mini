package com.mallmini.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("coupon")
public class Coupon {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String couponNo;

    private Long templateId;

    private Long userId;

    private String status;

    private String orderNo;

    private LocalDateTime usedAt;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
