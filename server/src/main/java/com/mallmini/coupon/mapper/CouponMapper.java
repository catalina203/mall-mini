package com.mallmini.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallmini.coupon.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    @Update("""
            UPDATE coupon
               SET status = 'USED',
                   order_no = #{orderNo},
                   used_at = NOW(3),
                   updated_at = NOW(3)
             WHERE id = #{couponId}
               AND user_id = #{userId}
               AND status = 'UNUSED'
               AND valid_from <= NOW(3)
               AND valid_to >= NOW(3)
            """)
    int lockCoupon(@Param("couponId") Long couponId,
                   @Param("userId") Long userId,
                   @Param("orderNo") String orderNo);

    @Update("""
            UPDATE coupon
               SET status = 'UNUSED',
                   order_no = NULL,
                   used_at = NULL,
                   updated_at = NOW(3)
             WHERE order_no = #{orderNo}
               AND status = 'USED'
            """)
    int releaseByOrderNo(@Param("orderNo") String orderNo);
}
