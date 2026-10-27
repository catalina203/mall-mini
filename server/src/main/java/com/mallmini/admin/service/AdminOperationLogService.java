package com.mallmini.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mallmini.admin.entity.AdminOperationLog;
import com.mallmini.admin.entity.AdminUser;
import com.mallmini.admin.mapper.AdminOperationLogMapper;
import com.mallmini.admin.mapper.AdminUserMapper;
import com.mallmini.admin.vo.AdminOperationLogVO;
import com.mallmini.common.api.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOperationLogService {

    private final AdminOperationLogMapper adminOperationLogMapper;
    private final AdminUserMapper adminUserMapper;

    public PageResult<AdminOperationLogVO> page(Long adminId,
                                                String action,
                                                String targetType,
                                                long page,
                                                long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<AdminOperationLog> wrapper = new LambdaQueryWrapper<AdminOperationLog>()
                .orderByDesc(AdminOperationLog::getCreatedAt)
                .orderByDesc(AdminOperationLog::getId);
        if (adminId != null) {
            wrapper.eq(AdminOperationLog::getAdminId, adminId);
        }
        if (action != null && !action.isBlank()) {
            wrapper.like(AdminOperationLog::getAction, action.trim());
        }
        if (targetType != null && !targetType.isBlank()) {
            wrapper.eq(AdminOperationLog::getTargetType, targetType.trim().toUpperCase());
        }
        Page<AdminOperationLog> result = adminOperationLogMapper.selectPage(
                new Page<>(safePage, safePageSize), wrapper);
        List<AdminOperationLog> logs = result.getRecords();
        if (logs.isEmpty()) {
            return PageResult.of(List.of(), safePage, safePageSize, result.getTotal());
        }
        Map<Long, AdminUser> admins = adminUserMapper.selectBatchIds(
                        logs.stream().map(AdminOperationLog::getAdminId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(AdminUser::getId, Function.identity()));
        List<AdminOperationLogVO> items = logs.stream().map(log -> {
            AdminUser admin = admins.get(log.getAdminId());
            return new AdminOperationLogVO(
                    log.getId(),
                    log.getAdminId(),
                    admin == null ? "" : admin.getUsername(),
                    log.getAction(),
                    log.getTargetType(),
                    log.getTargetId(),
                    log.getDetail(),
                    log.getRequestId(),
                    log.getCreatedAt());
        }).toList();
        return PageResult.of(items, safePage, safePageSize, result.getTotal());
    }
}
