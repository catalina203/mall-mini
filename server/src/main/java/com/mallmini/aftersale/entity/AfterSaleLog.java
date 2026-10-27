package com.mallmini.aftersale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("after_sale_log")
public class AfterSaleLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long afterSaleId;

    private String fromStatus;

    private String toStatus;

    private String operatorType;

    private Long operatorId;

    private String remark;

    private LocalDateTime createdAt;
}
