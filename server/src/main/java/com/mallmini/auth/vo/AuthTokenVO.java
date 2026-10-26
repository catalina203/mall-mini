package com.mallmini.auth.vo;

import com.mallmini.user.vo.UserVO;

public record AuthTokenVO(
        String token,
        boolean devMode,
        UserVO user
) {
}
