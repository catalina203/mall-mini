package com.mallmini.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mallmini.admin.service.AdminOperationLogger;
import com.mallmini.common.api.PageResult;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.member.entity.PointsLog;
import com.mallmini.member.enums.MemberLevel;
import com.mallmini.member.mapper.PointsLogMapper;
import com.mallmini.member.vo.MemberVO;
import com.mallmini.member.vo.PointsLogVO;
import com.mallmini.notification.service.NotificationService;
import com.mallmini.user.entity.User;
import com.mallmini.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PointsService {

    private final UserMapper userMapper;
    private final PointsLogMapper pointsLogMapper;
    private final NotificationService notificationService;
    private final AdminOperationLogger operationLogger;

    public MemberVO member(Long userId) {
        User user = requireUser(userId);
        MemberLevel level = MemberLevel.fromTotal(user.getTotalPoints() == null ? 0 : user.getTotalPoints());
        MemberLevel next = level.next();
        return new MemberVO(
                user.getPoints() == null ? 0 : user.getPoints(),
                user.getTotalPoints() == null ? 0 : user.getTotalPoints(),
                level.name(),
                level.label(),
                next == null ? null : next.threshold(),
                next == null ? null : next.label());
    }

    public PageResult<PointsLogVO> logs(Long userId, long page, long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        Page<PointsLog> result = pointsLogMapper.selectPage(
                new Page<>(safePage, safePageSize),
                new LambdaQueryWrapper<PointsLog>()
                        .eq(PointsLog::getUserId, userId)
                        .orderByDesc(PointsLog::getCreatedAt)
                        .orderByDesc(PointsLog::getId));
        List<PointsLogVO> items = result.getRecords().stream().map(log -> new PointsLogVO(
                log.getId(),
                log.getChangeType(),
                log.getPointsDelta() == null ? 0 : log.getPointsDelta(),
                log.getBalanceAfter() == null ? 0 : log.getBalanceAfter(),
                log.getRemark(),
                log.getCreatedAt())).toList();
        return PageResult.of(items, safePage, safePageSize, result.getTotal());
    }

    @Transactional
    public void awardOrderPoints(String orderNo, Long userId, BigDecimal payableAmount) {
        if (exists("ORDER_REWARD", orderNo, userId)) {
            return;
        }
        int points = payableAmount == null ? 0
                : payableAmount.setScale(0, RoundingMode.DOWN).intValue();
        if (points <= 0) {
            return;
        }
        User user = requireUser(userId);
        int before = user.getPoints() == null ? 0 : user.getPoints();
        int total = user.getTotalPoints() == null ? 0 : user.getTotalPoints();
        user.setPoints(before + points);
        user.setTotalPoints(total + points);
        user.setMemberLevel(MemberLevel.fromTotal(user.getTotalPoints()).name());
        userMapper.updateById(user);
        writeLog(userId, "ORDER_REWARD", points, user.getPoints(), "ORDER", orderNo,
                "订单完成奖励积分");
        notificationService.notifyUser(
                userId,
                "POINTS_REWARD",
                "积分已到账",
                "订单 " + orderNo + " 完成，获得 " + points + " 积分，当前余额 "
                        + user.getPoints() + " 分。",
                "ORDER",
                orderNo,
                Map.of("orderNo", orderNo, "points", points, "balance", user.getPoints()));
    }

    @Transactional
    public void revokeOrderPoints(String orderNo, Long userId) {
        PointsLog reward = pointsLogMapper.selectOne(new LambdaQueryWrapper<PointsLog>()
                .eq(PointsLog::getUserId, userId)
                .eq(PointsLog::getChangeType, "ORDER_REWARD")
                .eq(PointsLog::getBizNo, orderNo)
                .last("LIMIT 1"));
        if (reward == null || exists("ORDER_REFUND_REVOKE", orderNo, userId)) {
            return;
        }
        int rewardPoints = reward.getPointsDelta() == null ? 0 : reward.getPointsDelta();
        User user = requireUser(userId);
        int before = user.getPoints() == null ? 0 : user.getPoints();
        int total = user.getTotalPoints() == null ? 0 : user.getTotalPoints();
        user.setPoints(Math.max(before - rewardPoints, 0));
        user.setTotalPoints(Math.max(total - rewardPoints, 0));
        user.setMemberLevel(MemberLevel.fromTotal(user.getTotalPoints()).name());
        userMapper.updateById(user);
        writeLog(userId, "ORDER_REFUND_REVOKE", -rewardPoints, user.getPoints(), "ORDER", orderNo,
                "整单退款扣回积分");
    }

    @Transactional
    public void adjust(Long userId, int delta, String remark, Long adminId) {
        if (delta == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "积分变动不能为 0");
        }
        User user = requireUser(userId);
        int before = user.getPoints() == null ? 0 : user.getPoints();
        int total = user.getTotalPoints() == null ? 0 : user.getTotalPoints();
        if (delta < 0 && before + delta < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "扣减后积分不能为负");
        }
        user.setPoints(before + delta);
        user.setTotalPoints(Math.max(total + delta, 0));
        user.setMemberLevel(MemberLevel.fromTotal(user.getTotalPoints()).name());
        userMapper.updateById(user);
        writeLog(userId, "ADMIN_ADJUST", delta, user.getPoints(), "ADMIN",
                "ADJ" + IdWorker.getIdStr(),
                remark == null || remark.isBlank() ? "管理员调整" : remark.trim());
        operationLogger.log(adminId, "USER_POINTS", "USER", String.valueOf(userId),
                (delta > 0 ? "+" : "") + delta);
    }

    private void writeLog(Long userId,
                          String changeType,
                          int delta,
                          int balanceAfter,
                          String bizType,
                          String bizNo,
                          String remark) {
        PointsLog log = new PointsLog();
        log.setUserId(userId);
        log.setChangeType(changeType);
        log.setPointsDelta(delta);
        log.setBalanceAfter(balanceAfter);
        log.setBizType(bizType);
        log.setBizNo(bizNo);
        log.setRemark(remark);
        try {
            pointsLogMapper.insert(log);
        } catch (DuplicateKeyException ignored) {
            // 并发重复奖励按已处理
        }
    }

    private boolean exists(String changeType, String bizNo, Long userId) {
        return pointsLogMapper.selectCount(new LambdaQueryWrapper<PointsLog>()
                .eq(PointsLog::getChangeType, changeType)
                .eq(PointsLog::getBizNo, bizNo)
                .eq(PointsLog::getUserId, userId)) > 0;
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }
}
