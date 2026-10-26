package com.mallmini.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallmini.auth.dto.DevLoginRequest;
import com.mallmini.auth.dto.WechatLoginRequest;
import com.mallmini.auth.vo.AuthTokenVO;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.config.AppProperties;
import com.mallmini.security.JwtService;
import com.mallmini.user.entity.User;
import com.mallmini.user.enums.UserStatus;
import com.mallmini.user.mapper.UserMapper;
import com.mallmini.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final UserService userService;
    private final JwtService jwtService;
    private final AppProperties properties;
    private final RestClient.Builder restClientBuilder;

    @Transactional
    public AuthTokenVO devLogin(DevLoginRequest request) {
        if (!properties.isDevLoginEnabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "开发登录未启用");
        }
        String openId = request.openId().trim();
        String nickname = request.nickname() == null || request.nickname().isBlank()
                ? "开发用户" : request.nickname().trim();
        User user = findOrCreate(openId, nickname, null);
        requireActive(user);
        return new AuthTokenVO(jwtService.generate(user.getId(), JwtService.ROLE_USER), true, userService.toVO(user));
    }

    @Transactional
    public AuthTokenVO wechatLogin(WechatLoginRequest request) {
        String appId = properties.getWechat().getAppId();
        String appSecret = properties.getWechat().getAppSecret();
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "微信登录未配置，请使用开发登录");
        }

        WechatSessionResponse session = requestWechatSession(appId, appSecret, request.code().trim());
        if (session.openid() == null || session.openid().isBlank()) {
            String reason = session.errmsg() == null || session.errmsg().isBlank()
                    ? "微信登录失败" : "微信登录失败：" + session.errmsg();
            throw new BusinessException(ErrorCode.UNAUTHORIZED, reason);
        }

        User user = findOrCreate(session.openid(), "微信用户", session.unionid());
        requireActive(user);
        return new AuthTokenVO(jwtService.generate(user.getId(), JwtService.ROLE_USER), false, userService.toVO(user));
    }

    private User findOrCreate(String openId, String nickname, String unionId) {
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenId, openId));
        if (existing != null) {
            return existing;
        }

        User user = new User();
        user.setOpenId(openId);
        user.setUnionId(unionId);
        user.setNickname(nickname);
        user.setAvatarUrl("");
        user.setStatus(UserStatus.ACTIVE.name());
        try {
            userMapper.insert(user);
            return user;
        } catch (DuplicateKeyException ex) {
            User concurrent = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenId, openId));
            if (concurrent == null) {
                throw ex;
            }
            return concurrent;
        }
    }

    private void requireActive(User user) {
        if (!UserStatus.ACTIVE.name().equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
    }

    private WechatSessionResponse requestWechatSession(String appId, String appSecret, String code) {
        try {
            WechatSessionResponse response = restClientBuilder.build()
                    .get()
                    .uri(builder -> builder
                            .scheme("https")
                            .host("api.weixin.qq.com")
                            .path("/sns/jscode2session")
                            .queryParam("appid", appId)
                            .queryParam("secret", appSecret)
                            .queryParam("js_code", code)
                            .queryParam("grant_type", "authorization_code")
                            .build())
                    .retrieve()
                    .body(WechatSessionResponse.class);
            if (response == null) {
                throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "微信登录服务无响应");
            }
            return response;
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.warn("wechat login request failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "微信登录服务暂不可用");
        }
    }

    private record WechatSessionResponse(
            String openid,
            String unionid,
            Integer errcode,
            String errmsg
    ) {
    }
}
