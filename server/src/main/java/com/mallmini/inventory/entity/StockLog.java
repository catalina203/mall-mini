package com.mallmini.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stock_log")
public class StockLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long skuId;

    private Long productId;

    private String changeType;

    private Integer quantityDelta;

    private Integer stockBefore;

    private Integer stockAfter;

    private String bizType;

    private String bizNo;

    private String operatorType;

    private Long operatorId;

    private String remark;

    private LocalDateTime createdAt;
}
