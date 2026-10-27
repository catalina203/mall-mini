package com.mallmini.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mallmini.admin.service.AdminOperationLogger;
import com.mallmini.common.api.PageResult;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.common.util.MaskUtils;
import com.mallmini.user.entity.User;
import com.mallmini.user.enums.UserStatus;
import com.mallmini.user.mapper.UserMapper;
import com.mallmini.user.vo.AdminUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserMapper userMapper;
    private final AdminOperationLogger operationLogger;

    public PageResult<AdminUserVO> page(long page, long pageSize, String keyword, String status) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(User::getStatus, parseStatus(status).name());
        }
        if (keyword != null && !keyword.isBlank()) {
            String normalized = keyword.trim();
            wrapper.and(condition -> condition
                    .like(User::getNickname, normalized)
                    .or()
                    .like(User::getPhone, normalized));
        }

        Page<User> userPage = userMapper.selectPage(new Page<>(safePage, safePageSize), wrapper);
        List<AdminUserVO> items = userPage.getRecords().stream()
                .map(user -> new AdminUserVO(
                        user.getId(),
                        user.getNickname(),
                        user.getAvatarUrl(),
                        MaskUtils.maskPhone(user.getPhone()),
                        user.getStatus(),
                        user.getPoints() == null ? 0 : user.getPoints(),
                        user.getMemberLevel() == null ? "NORMAL" : user.getMemberLevel(),
                        user.getCreatedAt()))
                .toList();
        return PageResult.of(items, safePage, safePageSize, userPage.getTotal());
    }

    @Transactional
    public void updateStatus(Long userId, String statusValue, Long adminId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        UserStatus status = parseStatus(statusValue);
        String before = user.getStatus();
        user.setStatus(status.name());
        userMapper.updateById(user);
        operationLogger.log(adminId, "USER_STATUS", "USER", String.valueOf(userId), before + "->" + status.name());
    }

    private UserStatus parseStatus(String status) {
        try {
            return UserStatus.valueOf(status.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "用户状态不合法");
        }
    }
}
