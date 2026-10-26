package com.mallmini.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallmini.order.entity.MallOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface MallOrderMapper extends BaseMapper<MallOrder> {

    @Select("""
            SELECT COALESCE(SUM(payable_amount), 0)
              FROM mall_order
             WHERE payment_status = 'PAID'
               AND created_at >= #{start}
            """)
    BigDecimal sumPaidAmount(@Param("start") LocalDateTime start);

    @Update("""
            UPDATE mall_order
               SET status = 'CANCELLED',
                   cancelled_at = NOW(3),
                   cancel_reason = #{reason},
                   updated_at = NOW(3)
             WHERE id = #{orderId}
               AND status = #{fromStatus}
            """)
    int transitionToCancelled(@Param("orderId") Long orderId,
                              @Param("fromStatus") String fromStatus,
                              @Param("reason") String reason);

    @Update("""
            UPDATE mall_order
               SET status = 'PENDING_SHIPMENT',
                   payment_status = 'PAID',
                   paid_at = NOW(3),
                   updated_at = NOW(3)
             WHERE id = #{orderId}
               AND status = 'PENDING_PAYMENT'
               AND payment_status = 'UNPAID'
            """)
    int transitionToPaid(@Param("orderId") Long orderId);

    @Update("""
            UPDATE mall_order
               SET status = 'PENDING_RECEIPT',
                   logistics_company = #{company},
                   logistics_no = #{logisticsNo},
                   shipped_at = NOW(3),
                   updated_at = NOW(3)
             WHERE id = #{orderId}
               AND status = 'PENDING_SHIPMENT'
            """)
    int transitionToShipped(@Param("orderId") Long orderId,
                            @Param("company") String company,
                            @Param("logisticsNo") String logisticsNo);

    @Update("""
            UPDATE mall_order
               SET status = 'COMPLETED',
                   completed_at = NOW(3),
                   updated_at = NOW(3)
             WHERE id = #{orderId}
               AND status = 'PENDING_RECEIPT'
            """)
    int transitionToCompleted(@Param("orderId") Long orderId);
}
