package com.mallmini.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallmini.review.entity.ProductReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface ProductReviewMapper extends BaseMapper<ProductReview> {

    @Select("""
            SELECT COALESCE(AVG(rating), 0)
              FROM product_review
             WHERE product_id = #{productId}
               AND status = 'APPROVED'
            """)
    BigDecimal averageRating(@Param("productId") Long productId);

    @Select("""
            SELECT COUNT(*)
              FROM product_review
             WHERE product_id = #{productId}
               AND status = 'APPROVED'
            """)
    long approvedCount(@Param("productId") Long productId);
}
