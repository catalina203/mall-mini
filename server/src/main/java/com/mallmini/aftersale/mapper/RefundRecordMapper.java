package com.mallmini.aftersale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallmini.aftersale.entity.RefundRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface RefundRecordMapper extends BaseMapper<RefundRecord> {

    @Select("""
            SELECT COALESCE(SUM(amount), 0)
              FROM refund_record
             WHERE status = 'SUCCESS'
               AND success_at >= #{start}
            """)
    BigDecimal sumSuccessAmount(@Param("start") LocalDateTime start);
}
