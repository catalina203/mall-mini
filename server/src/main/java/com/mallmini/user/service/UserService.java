package com.mallmini.user.service;

import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.common.util.MaskUtils;
import com.mallmini.member.enums.MemberLevel;
import com.mallmini.user.entity.User;
import com.mallmini.user.enums.UserStatus;
import com.mallmini.user.mapper.UserMapper;
import com.mallmini.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public UserVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !UserStatus.ACTIVE.name().equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在或已被禁用");
        }
        return toVO(user);
    }

    public UserVO toVO(User user) {
        MemberLevel level = MemberLevel.fromTotal(
                user.getTotalPoints() == null ? 0 : user.getTotalPoints());
        return new UserVO(
                user.getId(),
                user.getNickname(),
                user.getAvatarUrl(),
                MaskUtils.maskPhone(user.getPhone()),
                user.getStatus(),
                user.getPoints() == null ? 0 : user.getPoints(),
                level.name(),
                level.label());
    }
}
