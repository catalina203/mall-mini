package com.mallmini.order.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallmini.config.AppProperties;
import com.mallmini.order.entity.MallOrder;
import com.mallmini.order.enums.OrderStatus;
import com.mallmini.order.mapper.MallOrderMapper;
import com.mallmini.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoConfirmJob {

    private final MallOrderMapper orderMapper;
    private final OrderService orderService;
    private final AppProperties properties;

    @Scheduled(fixedDelayString = "${app.order.auto-confirm-check-ms:1800000}", initialDelay = 60000)
    public void autoConfirm() {
        if (!properties.getOrder().isAutoConfirmEnabled()) {
            return;
        }
        int days = Math.max(properties.getOrder().getAutoConfirmDays(), 1);
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        List<MallOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getStatus, OrderStatus.PENDING_RECEIPT.name())
                .le(MallOrder::getShippedAt, threshold)
                .orderByAsc(MallOrder::getId)
                .last("LIMIT 100"));
        for (MallOrder order : orders) {
            try {
                if (orderService.autoConfirmReceipt(order.getId(), days)) {
                    log.info("auto completed order {}", order.getOrderNo());
                }
            } catch (Exception ex) {
                log.warn("failed to auto complete order {}: {}", order.getOrderNo(), ex.getMessage());
            }
        }
    }
}
