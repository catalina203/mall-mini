package com.mallmini.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("points_log")
public class PointsLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String changeType;

    private Integer pointsDelta;

    private Integer balanceAfter;

    private String bizType;

    private String bizNo;

    private String remark;

    private LocalDateTime createdAt;
}
