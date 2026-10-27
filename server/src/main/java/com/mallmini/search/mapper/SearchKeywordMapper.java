package com.mallmini.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallmini.search.entity.SearchKeyword;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SearchKeywordMapper extends BaseMapper<SearchKeyword> {

    @Insert("""
            INSERT INTO search_keyword (keyword, search_count, last_searched_at)
            VALUES (#{keyword}, 1, NOW(3))
            ON DUPLICATE KEY UPDATE
                search_count = search_count + 1,
                last_searched_at = NOW(3)
            """)
    int record(@Param("keyword") String keyword);

    @Select("""
            SELECT keyword
              FROM search_keyword
             ORDER BY search_count DESC, last_searched_at DESC
             LIMIT #{limit}
            """)
    List<String> hot(@Param("limit") int limit);
}
