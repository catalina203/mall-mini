package com.mallmini.auth.service;

import com.mallmini.auth.dto.DevLoginRequest;
import com.mallmini.auth.vo.AuthTokenVO;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.config.AppProperties;
import com.mallmini.security.JwtService;
import com.mallmini.user.entity.User;
import com.mallmini.user.mapper.UserMapper;
import com.mallmini.user.service.UserService;
import com.mallmini.user.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private RestClient.Builder restClientBuilder;

    private AppProperties properties;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        properties.setDevLoginEnabled(true);
        authService = new AuthService(userMapper, userService, jwtService, properties, restClientBuilder);
    }

    @Test
    void rejectsDevLoginWhenDisabled() {
        properties.setDevLoginEnabled(false);

        assertThatThrownBy(() -> authService.devLogin(new DevLoginRequest("dev-1", "开发用户")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("开发登录未启用");
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void createsUserAndReturnsToken() {
        UserVO userVO = new UserVO(1L, "开发用户", "", "", "ACTIVE");
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return 1;
        });
        when(jwtService.generate(1L, JwtService.ROLE_USER)).thenReturn("token-value");
        when(userService.toVO(any(User.class))).thenReturn(userVO);

        AuthTokenVO result = authService.devLogin(new DevLoginRequest("dev-1", "开发用户"));

        assertThat(result.token()).isEqualTo("token-value");
        assertThat(result.devMode()).isTrue();
        assertThat(result.user().nickname()).isEqualTo("开发用户");
    }

    @Test
    void reusesExistingUser() {
        User user = new User();
        user.setId(9L);
        user.setOpenId("dev-9");
        user.setStatus("ACTIVE");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(jwtService.generate(9L, JwtService.ROLE_USER)).thenReturn("token-9");
        when(userService.toVO(user)).thenReturn(new UserVO(9L, "老用户", "", "", "ACTIVE"));

        AuthTokenVO result = authService.devLogin(new DevLoginRequest("dev-9", "新昵称"));

        assertThat(result.token()).isEqualTo("token-9");
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void rejectsDisabledUser() {
        User user = new User();
        user.setId(5L);
        user.setOpenId("dev-5");
        user.setStatus("DISABLED");
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThatThrownBy(() -> authService.devLogin(new DevLoginRequest("dev-5", "禁用用户")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("禁用");
    }

    @Test
    void rejectsWechatLoginWithoutConfiguration() {
        properties.getWechat().setAppId("");
        properties.getWechat().setAppSecret("");

        assertThatThrownBy(() -> authService.wechatLogin(new com.mallmini.auth.dto.WechatLoginRequest("code")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("微信登录未配置");
    }
}
