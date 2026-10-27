package com.mallmini.shipping.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("shipping_template")
public class ShippingTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private BigDecimal defaultFee;

    private BigDecimal freeThreshold;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
