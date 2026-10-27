package com.mallmini.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallmini.coupon.entity.CouponTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {

    @Update("""
            UPDATE coupon_template
               SET claimed_count = claimed_count + 1,
                   updated_at = NOW(3)
             WHERE id = #{templateId}
               AND status = 'ENABLED'
               AND (total_count = 0 OR claimed_count < total_count)
            """)
    int claimOne(@Param("templateId") Long templateId);
}
