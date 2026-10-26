package com.mallmini.category.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallmini.category.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
