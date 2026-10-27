package com.mallmini.aftersale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("after_sale_item")
public class AfterSaleItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long afterSaleId;

    private Long orderItemId;

    private Long productId;

    private Long skuId;

    private String productName;

    private String skuSpec;

    private String productImage;

    private BigDecimal unitPrice;

    private Integer quantity;

    private BigDecimal refundAmount;

    private LocalDateTime createdAt;
}
