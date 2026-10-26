package com.mallmini.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallmini.cart.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}
