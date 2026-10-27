package com.mallmini.search.service;

import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.search.mapper.SearchKeywordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchKeywordMapper searchKeywordMapper;

    public void record(String keyword) {
        String normalized = normalize(keyword);
        if (normalized == null) {
            return;
        }
        searchKeywordMapper.record(normalized);
    }

    public List<String> hot(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        return searchKeywordMapper.hot(safeLimit);
    }

    private String normalize(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String normalized = keyword.trim();
        if (normalized.length() > 64) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "关键词过长");
        }
        return normalized;
    }
}
