package com.mallmini.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification_log")
public class NotificationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String channel;

    private String recipientType;

    private Long recipientId;

    private String templateCode;

    private String title;

    private String content;

    private String bizType;

    private String bizNo;

    private String payloadJson;

    private String status;

    private Integer retryCount;

    private LocalDateTime readAt;

    private LocalDateTime sentAt;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
