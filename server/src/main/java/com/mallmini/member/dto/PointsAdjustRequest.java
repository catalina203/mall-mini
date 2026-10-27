package com.mallmini.member.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PointsAdjustRequest(
        @NotNull(message = "积分变动不能为空")
        Integer delta,

        @Size(max = 255, message = "备注过长")
        String remark
) {
}
