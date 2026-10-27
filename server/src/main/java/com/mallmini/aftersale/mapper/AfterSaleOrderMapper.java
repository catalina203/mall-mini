package com.mallmini.aftersale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallmini.aftersale.entity.AfterSaleOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AfterSaleOrderMapper extends BaseMapper<AfterSaleOrder> {

    @Select("""
            SELECT COUNT(*)
              FROM after_sale_order
             WHERE status IN ('APPLIED', 'APPROVED', 'WAIT_RETURN', 'RETURNED',
                              'RETURN_RECEIVED', 'REFUNDING', 'REFUND_FAILED')
            """)
    long countActive();

    @Update("""
            UPDATE after_sale_order
               SET status = 'CANCELLED',
                   closed_at = NOW(3),
                   updated_at = NOW(3)
             WHERE id = #{id}
               AND status IN ('APPLIED', 'APPROVED', 'WAIT_RETURN')
            """)
    int cancel(@Param("id") Long id);

    @Update("""
            UPDATE after_sale_order
               SET status = #{targetStatus},
                   reviewed_by = #{adminId},
                   reviewed_at = NOW(3),
                   review_remark = '审核通过',
                   updated_at = NOW(3)
             WHERE id = #{id}
               AND status = 'APPLIED'
            """)
    int approve(@Param("id") Long id,
                @Param("targetStatus") String targetStatus,
                @Param("adminId") Long adminId);

    @Update("""
            UPDATE after_sale_order
               SET status = 'REJECTED',
                   reviewed_by = #{adminId},
                   reviewed_at = NOW(3),
                   review_remark = #{reason},
                   closed_at = NOW(3),
                   updated_at = NOW(3)
             WHERE id = #{id}
               AND status = 'APPLIED'
            """)
    int reject(@Param("id") Long id,
               @Param("adminId") Long adminId,
               @Param("reason") String reason);

    @Update("""
            UPDATE after_sale_order
               SET status = 'RETURNED',
                   return_company = #{company},
                   return_tracking_no = #{trackingNo},
                   returned_at = NOW(3),
                   updated_at = NOW(3)
             WHERE id = #{id}
               AND status = 'WAIT_RETURN'
            """)
    int submitReturn(@Param("id") Long id,
                     @Param("company") String company,
                     @Param("trackingNo") String trackingNo);

    @Update("""
            UPDATE after_sale_order
               SET status = 'RETURN_RECEIVED',
                   review_remark = '已确认收到退货',
                   updated_at = NOW(3)
             WHERE id = #{id}
               AND status = 'RETURNED'
            """)
    int confirmReturn(@Param("id") Long id);

    @Update("""
            UPDATE after_sale_order
               SET status = 'REFUNDING',
                   updated_at = NOW(3)
             WHERE id = #{id}
               AND status = #{expectedStatus}
            """)
    int startRefunding(@Param("id") Long id,
                       @Param("expectedStatus") String expectedStatus);

    @Update("""
            UPDATE after_sale_order
               SET status = 'REFUNDED',
                   refunded_at = NOW(3),
                   closed_at = NOW(3),
                   updated_at = NOW(3)
             WHERE id = #{id}
               AND status = 'REFUNDING'
            """)
    int markRefunded(@Param("id") Long id);

    @Update("""
            UPDATE after_sale_order
               SET status = 'REFUND_FAILED',
                   updated_at = NOW(3)
             WHERE id = #{id}
               AND status = 'REFUNDING'
            """)
    int markRefundFailed(@Param("id") Long id);
}
