package com.mallmini.search.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SearchKeywordRequest(
        @NotBlank(message = "关键词不能为空")
        @Size(max = 64, message = "关键词过长")
        String keyword
) {
}
