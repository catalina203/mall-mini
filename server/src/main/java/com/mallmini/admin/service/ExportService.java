package com.mallmini.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallmini.common.util.CsvUtils;
import com.mallmini.common.util.MaskUtils;
import com.mallmini.inventory.service.StockService;
import com.mallmini.inventory.vo.StockLogVO;
import com.mallmini.order.entity.MallOrder;
import com.mallmini.order.mapper.MallOrderMapper;
import com.mallmini.user.entity.User;
import com.mallmini.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private static final int MAX_ROWS = 10000;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MallOrderMapper orderMapper;
    private final UserMapper userMapper;
    private final StockService stockService;
    private final AdminOperationLogger operationLogger;

    public void exportOrders(String status,
                             String orderNo,
                             Long userId,
                             Long adminId,
                             HttpServletResponse response) throws IOException {
        LambdaQueryWrapper<MallOrder> wrapper = new LambdaQueryWrapper<MallOrder>()
                .orderByDesc(MallOrder::getCreatedAt)
                .orderByDesc(MallOrder::getId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(MallOrder::getStatus, status.trim().toUpperCase());
        }
        if (orderNo != null && !orderNo.isBlank()) {
            wrapper.like(MallOrder::getOrderNo, orderNo.trim());
        }
        if (userId != null) {
            wrapper.eq(MallOrder::getUserId, userId);
        }
        wrapper.last("LIMIT " + MAX_ROWS);
        List<MallOrder> orders = orderMapper.selectList(wrapper);

        List<String> headers = List.of(
                "订单号", "用户ID", "订单状态", "支付状态", "商品金额", "运费", "优惠", "应付金额",
                "收货人", "手机号", "省", "市", "区", "详细地址", "物流公司", "物流单号",
                "下单时间", "支付时间", "发货时间", "完成时间", "取消原因", "后台备注");
        List<List<Object>> rows = new ArrayList<>();
        for (MallOrder order : orders) {
            rows.add(List.<Object>of(
                    order.getOrderNo(),
                    order.getUserId(),
                    order.getStatus(),
                    order.getPaymentStatus(),
                    order.getGoodsAmount(),
                    order.getShippingFee(),
                    order.getDiscountAmount(),
                    order.getPayableAmount(),
                    order.getReceiverName(),
                    MaskUtils.maskPhone(order.getReceiverPhone()),
                    order.getReceiverProvince(),
                    order.getReceiverCity(),
                    order.getReceiverDistrict(),
                    order.getReceiverDetail(),
                    nullSafe(order.getLogisticsCompany()),
                    nullSafe(order.getLogisticsNo()),
                    time(order.getCreatedAt()),
                    time(order.getPaidAt()),
                    time(order.getShippedAt()),
                    time(order.getCompletedAt()),
                    nullSafe(order.getCancelReason()),
                    nullSafe(order.getAdminRemark())));
        }
        CsvUtils.write(response, "orders.csv", headers, rows);
        operationLogger.log(adminId, "ORDER_EXPORT", "ORDER", "", "导出订单 " + orders.size() + " 条");
    }

    public void exportUsers(String keyword,
                            String status,
                            Long adminId,
                            HttpServletResponse response) throws IOException {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(User::getStatus, status.trim().toUpperCase());
        }
        if (keyword != null && !keyword.isBlank()) {
            String normalized = keyword.trim();
            wrapper.and(condition -> condition
                    .like(User::getNickname, normalized)
                    .or()
                    .like(User::getPhone, normalized));
        }
        wrapper.last("LIMIT " + MAX_ROWS);
        List<User> users = userMapper.selectList(wrapper);

        List<String> headers = List.of("用户ID", "昵称", "手机号（脱敏）", "状态", "注册时间");
        List<List<Object>> rows = users.stream().map(user -> List.<Object>of(
                user.getId(),
                nullSafe(user.getNickname()),
                MaskUtils.maskPhone(user.getPhone()),
                user.getStatus(),
                time(user.getCreatedAt()))).toList();
        CsvUtils.write(response, "users.csv", headers, rows);
        operationLogger.log(adminId, "USER_EXPORT", "USER", "", "导出用户 " + users.size() + " 条");
    }

    public void exportStockLogs(Long skuId,
                                String bizNo,
                                String changeType,
                                Long adminId,
                                HttpServletResponse response) throws IOException {
        List<StockLogVO> logs = stockService.exportList(skuId, bizNo, changeType, MAX_ROWS);
        List<String> headers = List.of(
                "时间", "SKU ID", "SKU 编码", "商品", "变动类型", "变动数量",
                "变动前", "变动后", "业务类型", "业务单号", "操作人", "备注");
        List<List<Object>> rows = logs.stream().map(log -> List.<Object>of(
                time(log.createdAt()),
                log.skuId(),
                nullSafe(log.skuCode()),
                nullSafe(log.productName()),
                log.changeType(),
                log.quantityDelta(),
                log.stockBefore(),
                log.stockAfter(),
                nullSafe(log.bizType()),
                nullSafe(log.bizNo()),
                log.operatorType() + (log.operatorId() == null ? "" : " #" + log.operatorId()),
                nullSafe(log.remark()))).toList();
        CsvUtils.write(response, "stock-logs.csv", headers, rows);
        operationLogger.log(adminId, "STOCK_EXPORT", "STOCK", "", "导出库存流水 " + logs.size() + " 条");
    }

    private String time(LocalDateTime value) {
        return value == null ? "" : value.format(TIME);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
