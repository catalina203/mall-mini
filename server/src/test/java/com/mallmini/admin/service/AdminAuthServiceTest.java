package com.mallmini.admin.service;

import com.mallmini.admin.dto.AdminLoginRequest;
import com.mallmini.admin.entity.AdminUser;
import com.mallmini.admin.mapper.AdminUserMapper;
import com.mallmini.admin.vo.AdminTokenVO;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private AdminUserMapper adminUserMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AdminAuthService adminAuthService;

    @Test
    void loginReturnsTokenAndUpdatesLastLogin() {
        AdminUser admin = admin("ACTIVE", "hash");
        when(adminUserMapper.selectOne(any())).thenReturn(admin);
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(jwtService.generate(1L, JwtService.ROLE_ADMIN)).thenReturn("admin-token");

        AdminTokenVO result = adminAuthService.login(new AdminLoginRequest("admin", "secret"));

        assertThat(result.token()).isEqualTo("admin-token");
        assertThat(result.admin().username()).isEqualTo("admin");
        verify(adminUserMapper).updateById(admin);
        assertThat(admin.getLastLoginAt()).isNotNull();
    }

    @Test
    void wrongPasswordIsRejected() {
        when(adminUserMapper.selectOne(any())).thenReturn(admin("ACTIVE", "hash"));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> adminAuthService.login(new AdminLoginRequest("admin", "wrong")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");
        verify(jwtService, never()).generate(any(), any());
    }

    @Test
    void disabledAdminIsRejected() {
        AdminUser admin = admin("DISABLED", "hash");
        when(adminUserMapper.selectOne(any())).thenReturn(admin);

        assertThatThrownBy(() -> adminAuthService.login(new AdminLoginRequest("admin", "secret")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");
    }

    private AdminUser admin(String status, String passwordHash) {
        AdminUser admin = new AdminUser();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setDisplayName("管理员");
        admin.setPasswordHash(passwordHash);
        admin.setRole("ADMIN");
        admin.setStatus(status);
        return admin;
    }
}
