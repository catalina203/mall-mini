package com.mallmini.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallmini.dashboard.vo.DashboardTrendRow;
import com.mallmini.order.entity.MallOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MallOrderMapper extends BaseMapper<MallOrder> {

    @Select("""
            SELECT COALESCE(SUM(payable_amount), 0)
              FROM mall_order
             WHERE payment_status IN ('PAID', 'PARTIALLY_REFUNDED', 'REFUNDED')
               AND paid_at >= #{start}
            """)
    BigDecimal sumPaidAmount(@Param("start") LocalDateTime start);

    @Select("""
            SELECT COUNT(*)
              FROM mall_order
             WHERE payment_status IN ('PAID', 'PARTIALLY_REFUNDED', 'REFUNDED')
               AND paid_at >= #{start}
            """)
    long countPaidOrders(@Param("start") LocalDateTime start);

    @Select("""
            SELECT DATE(paid_at) AS day,
                   COUNT(*) AS order_count,
                   COALESCE(SUM(payable_amount), 0) AS paid_amount
              FROM mall_order
             WHERE payment_status IN ('PAID', 'PARTIALLY_REFUNDED', 'REFUNDED')
               AND paid_at >= #{start}
             GROUP BY DATE(paid_at)
             ORDER BY day
            """)
    List<DashboardTrendRow> trend(@Param("start") LocalDateTime start);

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

    @Update("""
            UPDATE mall_order
               SET payment_status = #{status},
                   updated_at = NOW(3)
             WHERE id = #{orderId}
               AND payment_status IN ('PAID', 'PARTIALLY_REFUNDED')
            """)
    int updatePaymentStatus(@Param("orderId") Long orderId,
                            @Param("status") String status);

    @Update("""
            UPDATE mall_order
               SET receiver_name = #{receiverName},
                   receiver_phone = #{receiverPhone},
                   receiver_province = #{province},
                   receiver_city = #{city},
                   receiver_district = #{district},
                   receiver_detail = #{detail},
                   updated_at = NOW(3)
             WHERE id = #{orderId}
               AND status = 'PENDING_SHIPMENT'
               AND payment_status IN ('PAID', 'PARTIALLY_REFUNDED')
            """)
    int updateReceiver(@Param("orderId") Long orderId,
                       @Param("receiverName") String receiverName,
                       @Param("receiverPhone") String receiverPhone,
                       @Param("province") String province,
                       @Param("city") String city,
                       @Param("district") String district,
                       @Param("detail") String detail);

    @Update("""
            UPDATE mall_order
               SET admin_remark = #{remark},
                   updated_at = NOW(3)
             WHERE id = #{orderId}
            """)
    int updateAdminRemark(@Param("orderId") Long orderId, @Param("remark") String remark);
}
