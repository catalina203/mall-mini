package com.mallmini.support.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("support_ticket")
public class SupportTicket {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ticketNo;

    private Long userId;

    private String orderNo;

    private String type;

    private String subject;

    private String content;

    private String status;

    private String reply;

    private Long repliedBy;

    private LocalDateTime repliedAt;

    private LocalDateTime closedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
