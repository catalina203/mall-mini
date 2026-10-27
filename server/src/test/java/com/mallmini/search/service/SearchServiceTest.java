package com.mallmini.search.service;

import com.mallmini.search.mapper.SearchKeywordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private SearchKeywordMapper searchKeywordMapper;

    @InjectMocks
    private SearchService searchService;

    @Test
    void recordTrimsKeyword() {
        searchService.record("  耳机  ");

        verify(searchKeywordMapper).record("耳机");
    }

    @Test
    void hotCapsLimit() {
        when(searchKeywordMapper.hot(20)).thenReturn(List.of("耳机"));

        assertThat(searchService.hot(100)).containsExactly("耳机");
    }
}
