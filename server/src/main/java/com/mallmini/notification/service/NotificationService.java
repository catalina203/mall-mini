package com.mallmini.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mallmini.common.api.PageResult;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.notification.entity.NotificationLog;
import com.mallmini.notification.enums.NotificationChannel;
import com.mallmini.notification.enums.NotificationRecipientType;
import com.mallmini.notification.enums.NotificationStatus;
import com.mallmini.notification.mapper.NotificationLogMapper;
import com.mallmini.notification.vo.AdminNotificationVO;
import com.mallmini.notification.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationLogMapper notificationLogMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public void notifyUser(Long userId,
                           String templateCode,
                           String title,
                           String content,
                           String bizType,
                           String bizNo,
                           Map<String, Object> payload) {
        insert(NotificationRecipientType.USER, userId, templateCode, title, content,
                bizType, bizNo, payload);
    }

    @Transactional
    public void notifyAdmins(String templateCode,
                             String title,
                             String content,
                             String bizType,
                             String bizNo,
                             Map<String, Object> payload) {
        insert(NotificationRecipientType.ADMIN, 0L, templateCode, title, content,
                bizType, bizNo, payload);
    }

    public PageResult<NotificationVO> pageForUser(Long userId, long page, long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        Page<NotificationLog> result = notificationLogMapper.selectPage(
                new Page<>(safePage, safePageSize),
                new LambdaQueryWrapper<NotificationLog>()
                        .eq(NotificationLog::getRecipientType, NotificationRecipientType.USER.name())
                        .eq(NotificationLog::getRecipientId, userId)
                        .orderByDesc(NotificationLog::getCreatedAt)
                        .orderByDesc(NotificationLog::getId));
        List<NotificationVO> items = result.getRecords().stream()
                .map(log -> new NotificationVO(
                        log.getId(),
                        log.getTemplateCode(),
                        log.getTitle(),
                        log.getContent(),
                        log.getBizType(),
                        log.getBizNo(),
                        log.getReadAt() != null,
                        log.getReadAt(),
                        log.getCreatedAt()))
                .toList();
        return PageResult.of(items, safePage, safePageSize, result.getTotal());
    }

    public long unreadCount(Long userId) {
        return notificationLogMapper.selectCount(new LambdaQueryWrapper<NotificationLog>()
                .eq(NotificationLog::getRecipientType, NotificationRecipientType.USER.name())
                .eq(NotificationLog::getRecipientId, userId)
                .isNull(NotificationLog::getReadAt));
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        NotificationLog log = notificationLogMapper.selectById(notificationId);
        if (log == null
                || !NotificationRecipientType.USER.name().equals(log.getRecipientType())
                || !userId.equals(log.getRecipientId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "通知不存在");
        }
        if (log.getReadAt() == null) {
            log.setReadAt(LocalDateTime.now());
            notificationLogMapper.updateById(log);
        }
    }

    public PageResult<AdminNotificationVO> adminPage(String status,
                                                     String templateCode,
                                                     long page,
                                                     long pageSize) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<NotificationLog> wrapper = new LambdaQueryWrapper<NotificationLog>()
                .orderByDesc(NotificationLog::getCreatedAt)
                .orderByDesc(NotificationLog::getId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(NotificationLog::getStatus, status.trim().toUpperCase());
        }
        if (templateCode != null && !templateCode.isBlank()) {
            wrapper.like(NotificationLog::getTemplateCode, templateCode.trim());
        }
        Page<NotificationLog> result = notificationLogMapper.selectPage(
                new Page<>(safePage, safePageSize), wrapper);
        List<AdminNotificationVO> items = result.getRecords().stream()
                .map(log -> new AdminNotificationVO(
                        log.getId(),
                        log.getChannel(),
                        log.getRecipientType(),
                        log.getRecipientId(),
                        log.getTemplateCode(),
                        log.getTitle(),
                        log.getContent(),
                        log.getBizType(),
                        log.getBizNo(),
                        log.getStatus(),
                        log.getRetryCount() == null ? 0 : log.getRetryCount(),
                        log.getErrorMessage(),
                        log.getSentAt(),
                        log.getCreatedAt()))
                .toList();
        return PageResult.of(items, safePage, safePageSize, result.getTotal());
    }

    private void insert(NotificationRecipientType recipientType,
                        Long recipientId,
                        String templateCode,
                        String title,
                        String content,
                        String bizType,
                        String bizNo,
                        Map<String, Object> payload) {
        NotificationLog log = new NotificationLog();
        log.setChannel(NotificationChannel.IN_APP.name());
        log.setRecipientType(recipientType.name());
        log.setRecipientId(recipientId);
        log.setTemplateCode(templateCode);
        log.setTitle(title);
        log.setContent(content == null ? "" : content);
        log.setBizType(bizType == null ? "" : bizType);
        log.setBizNo(bizNo == null ? "" : bizNo);
        log.setPayloadJson(toJson(payload));
        log.setStatus(NotificationStatus.SENT.name());
        log.setRetryCount(0);
        log.setSentAt(LocalDateTime.now());
        notificationLogMapper.insert(log);
    }

    private String toJson(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
