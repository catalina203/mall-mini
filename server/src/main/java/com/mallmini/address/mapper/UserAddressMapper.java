package com.mallmini.address.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallmini.address.entity.UserAddress;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddress> {
}
