package com.mallmini.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon_template")
public class CouponTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String type;

    private BigDecimal value;

    private BigDecimal minAmount;

    private String scope;

    private Long scopeId;

    private Integer totalCount;

    private Integer claimedCount;

    private Integer perUserLimit;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
