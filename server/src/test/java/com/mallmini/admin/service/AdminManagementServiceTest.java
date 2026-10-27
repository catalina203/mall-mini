package com.mallmini.admin.service;

import com.mallmini.admin.dto.AdminCreateRequest;
import com.mallmini.admin.dto.AdminUpdateRequest;
import com.mallmini.admin.entity.AdminUser;
import com.mallmini.admin.mapper.AdminUserMapper;
import com.mallmini.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminManagementServiceTest {

    @Mock
    private AdminUserMapper adminUserMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AdminOperationLogger operationLogger;

    private AdminManagementService service;

    @BeforeEach
    void setUp() {
        service = new AdminManagementService(adminUserMapper, passwordEncoder, operationLogger);
    }

    @Test
    void createHashesPasswordAndAssignsRole() {
        when(adminUserMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("Password123")).thenReturn("hash");
        when(adminUserMapper.insert(any(AdminUser.class))).thenAnswer(invocation -> {
            AdminUser admin = invocation.getArgument(0);
            admin.setId(5L);
            return 1;
        });

        var result = service.create(new AdminCreateRequest(
                "operator1", "Password123", "运营一号", "OPERATOR"), 1L);

        assertThat(result.username()).isEqualTo("operator1");
        assertThat(result.role()).isEqualTo("OPERATOR");
        assertThat(result.permissions()).contains("ORDER_READ");
        verify(adminUserMapper).insert(any(AdminUser.class));
        verify(operationLogger).log(eq(1L), eq("ADMIN_CREATE"), eq("ADMIN"), eq("5"), any());
    }

    @Test
    void cannotDemoteLastActiveSuperAdmin() {
        AdminUser superAdmin = admin(1L, "SUPER_ADMIN", "ACTIVE");
        when(adminUserMapper.selectById(1L)).thenReturn(superAdmin);
        when(adminUserMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> service.update(1L,
                new AdminUpdateRequest("超管", "OPERATOR"), 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("至少保留一个");
        verify(adminUserMapper, never()).updateById(any(AdminUser.class));
    }

    @Test
    void cannotDisableSelf() {
        AdminUser admin = admin(1L, "OPERATOR", "ACTIVE");
        when(adminUserMapper.selectById(1L)).thenReturn(admin);

        assertThatThrownBy(() -> service.updateStatus(1L, "DISABLED", 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能禁用自己");
    }

    @Test
    void changePasswordRejectsWrongOldPassword() {
        AdminUser admin = admin(1L, "OPERATOR", "ACTIVE");
        when(adminUserMapper.selectById(1L)).thenReturn(admin);
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(1L, "wrong", "NewPassword123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("原密码不正确");
    }

    private AdminUser admin(Long id, String role, String status) {
        AdminUser admin = new AdminUser();
        admin.setId(id);
        admin.setUsername("admin" + id);
        admin.setDisplayName("管理员");
        admin.setPasswordHash("hash");
        admin.setRole(role);
        admin.setStatus(status);
        admin.setTokenVersion(0);
        return admin;
    }
}
