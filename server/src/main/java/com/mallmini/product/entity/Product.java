package com.mallmini.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String name;

    private String subtitle;

    private String description;

    private String mainImage;

    private String status;

    private Integer sortOrder;

    private Long shippingTemplateId;

    private Integer salesCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
