package com.mallmini.shipping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("shipping_template_rule")
public class ShippingTemplateRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private String regionCode;

    private Integer firstUnit;

    private BigDecimal firstFee;

    private Integer additionalUnit;

    private BigDecimal additionalFee;

    private BigDecimal freeThreshold;

    private String status;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
