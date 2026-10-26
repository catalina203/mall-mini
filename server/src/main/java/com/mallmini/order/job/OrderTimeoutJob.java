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
public class OrderTimeoutJob {

    private final MallOrderMapper orderMapper;
    private final OrderService orderService;
    private final AppProperties properties;

    @Scheduled(fixedDelayString = "${app.order.auto-cancel-check-ms:300000}", initialDelay = 60000)
    public void cancelExpiredOrders() {
        if (!properties.getOrder().isAutoCancelEnabled()) {
            return;
        }
        int cancelMinutes = properties.getOrder().getCancelMinutes();
        if (cancelMinutes <= 0) {
            return;
        }

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(cancelMinutes);
        List<MallOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getStatus, OrderStatus.PENDING_PAYMENT.name())
                .le(MallOrder::getCreatedAt, threshold)
                .orderByAsc(MallOrder::getId)
                .last("LIMIT 100"));

        for (MallOrder order : orders) {
            try {
                orderService.systemCancel(order.getId());
                log.info("auto cancelled expired order {}", order.getOrderNo());
            } catch (Exception ex) {
                log.warn("failed to auto cancel order {}: {}", order.getOrderNo(), ex.getMessage());
            }
        }
    }
}
