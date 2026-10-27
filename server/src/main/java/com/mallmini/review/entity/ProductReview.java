package com.mallmini.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_review")
public class ProductReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long orderItemId;

    private Long productId;

    private Long skuId;

    private Long userId;

    private Integer rating;

    private String content;

    private Boolean anonymous;

    private String status;

    private String reply;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
