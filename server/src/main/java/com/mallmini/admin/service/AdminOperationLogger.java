package com.mallmini.admin.service;

import com.mallmini.admin.entity.AdminOperationLog;
import com.mallmini.admin.mapper.AdminOperationLogMapper;
import com.mallmini.common.web.RequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminOperationLogger {

    private final AdminOperationLogMapper adminOperationLogMapper;

    public void log(Long adminId, String action, String targetType, String targetId, String detail) {
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(adminId);
        log.setAction(action);
        log.setTargetType(targetType == null ? "" : targetType);
        log.setTargetId(targetId == null ? "" : targetId);
        log.setDetail(truncate(detail, 512));
        log.setRequestId(RequestContext.requestId());
        adminOperationLogMapper.insert(log);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
