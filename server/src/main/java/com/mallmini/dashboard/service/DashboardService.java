package com.mallmini.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallmini.aftersale.mapper.AfterSaleOrderMapper;
import com.mallmini.aftersale.mapper.RefundRecordMapper;
import com.mallmini.config.AppProperties;
import com.mallmini.dashboard.vo.DashboardSummaryVO;
import com.mallmini.dashboard.vo.DashboardTrendRow;
import com.mallmini.dashboard.vo.DashboardTrendVO;
import com.mallmini.order.entity.MallOrder;
import com.mallmini.order.enums.OrderStatus;
import com.mallmini.order.mapper.MallOrderMapper;
import com.mallmini.product.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final MallOrderMapper orderMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final AfterSaleOrderMapper afterSaleOrderMapper;
    private final ProductSkuMapper productSkuMapper;
    private final AppProperties properties;

    public DashboardSummaryVO summary(int days) {
        int safeDays = Math.min(Math.max(days, 1), 365);
        LocalDateTime end = LocalDateTime.now();
        LocalDate startDate = LocalDate.now().minusDays(safeDays - 1L);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        long orderCount = orderMapper.selectCount(new LambdaQueryWrapper<MallOrder>()
                .ge(MallOrder::getCreatedAt, start));
        long paidOrderCount = orderMapper.countPaidOrders(start);
        long pendingShipment = orderMapper.selectCount(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getStatus, OrderStatus.PENDING_SHIPMENT.name()));
        long pendingReceipt = orderMapper.selectCount(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getStatus, OrderStatus.PENDING_RECEIPT.name()));
        long todayOrderCount = orderMapper.selectCount(new LambdaQueryWrapper<MallOrder>()
                .ge(MallOrder::getCreatedAt, todayStart));

        BigDecimal paidAmount = money(orderMapper.sumPaidAmount(start));
        BigDecimal refundAmount = money(refundRecordMapper.sumSuccessAmount(start));
        BigDecimal netAmount = paidAmount.subtract(refundAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal averageOrderValue = paidOrderCount == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : netAmount.divide(BigDecimal.valueOf(paidOrderCount), 2, RoundingMode.HALF_UP);
        BigDecimal refundRate = paidAmount.signum() == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : refundAmount.multiply(HUNDRED).divide(paidAmount, 2, RoundingMode.HALF_UP);

        Map<LocalDate, DashboardTrendRow> trendRows = orderMapper.trend(start).stream()
                .collect(Collectors.toMap(DashboardTrendRow::getDay, Function.identity()));
        List<DashboardTrendVO> trend = new ArrayList<>();
        for (int i = 0; i < safeDays; i++) {
            LocalDate day = startDate.plusDays(i);
            DashboardTrendRow row = trendRows.get(day);
            trend.add(new DashboardTrendVO(
                    day.toString(),
                    row == null || row.getOrderCount() == null ? 0 : row.getOrderCount(),
                    row == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                            : money(row.getPaidAmount())));
        }

        return new DashboardSummaryVO(
                safeDays,
                start,
                end,
                end,
                orderCount,
                paidOrderCount,
                paidAmount,
                refundAmount,
                netAmount,
                averageOrderValue,
                refundRate,
                pendingShipment,
                pendingReceipt,
                afterSaleOrderMapper.countActive(),
                productSkuMapper.countLowStock(properties.getStock().getAlertThreshold()),
                todayOrderCount,
                trend);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }
}
