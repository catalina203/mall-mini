package com.mallmini.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallmini.dashboard.vo.DashboardSummaryVO;
import com.mallmini.order.entity.MallOrder;
import com.mallmini.order.enums.OrderStatus;
import com.mallmini.order.mapper.MallOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MallOrderMapper orderMapper;

    public DashboardSummaryVO summary(int days) {
        int safeDays = Math.min(Math.max(days, 1), 365);
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = LocalDate.now().minusDays(safeDays - 1L).atStartOfDay();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        long orderCount = orderMapper.selectCount(new LambdaQueryWrapper<MallOrder>()
                .ge(MallOrder::getCreatedAt, start));
        long pendingShipment = orderMapper.selectCount(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getStatus, OrderStatus.PENDING_SHIPMENT.name()));
        long pendingReceipt = orderMapper.selectCount(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getStatus, OrderStatus.PENDING_RECEIPT.name()));
        long todayOrderCount = orderMapper.selectCount(new LambdaQueryWrapper<MallOrder>()
                .ge(MallOrder::getCreatedAt, todayStart));
        BigDecimal paidAmount = orderMapper.sumPaidAmount(start);

        return new DashboardSummaryVO(
                safeDays,
                start,
                end,
                end,
                orderCount,
                paidAmount == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                        : paidAmount.setScale(2, RoundingMode.HALF_UP),
                pendingShipment,
                pendingReceipt,
                todayOrderCount);
    }
}
