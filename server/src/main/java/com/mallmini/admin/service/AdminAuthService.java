package com.mallmini.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallmini.admin.dto.AdminLoginRequest;
import com.mallmini.admin.entity.AdminUser;
import com.mallmini.admin.enums.AdminStatus;
import com.mallmini.admin.mapper.AdminUserMapper;
import com.mallmini.admin.vo.AdminTokenVO;
import com.mallmini.admin.vo.AdminVO;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import com.mallmini.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AdminTokenVO login(AdminLoginRequest request) {
        AdminUser admin = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, request.username().trim()));
        if (admin == null
                || !AdminStatus.ACTIVE.name().equals(admin.getStatus())
                || !passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        admin.setLastLoginAt(LocalDateTime.now());
        adminUserMapper.updateById(admin);
        int tokenVersion = admin.getTokenVersion() == null ? 0 : admin.getTokenVersion();
        return new AdminTokenVO(jwtService.generateAdmin(admin.getId(), tokenVersion), toVO(admin));
    }

    public AdminVO me(Long adminId) {
        AdminUser admin = adminUserMapper.selectById(adminId);
        if (admin == null || !AdminStatus.ACTIVE.name().equals(admin.getStatus())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "管理员不存在或已禁用");
        }
        return toVO(admin);
    }

    public AdminVO toVO(AdminUser admin) {
        return new AdminVO(
                admin.getId(),
                admin.getUsername(),
                admin.getDisplayName(),
                admin.getRole(),
                admin.getStatus(),
                AdminPermissionService.permissionCodes(admin.getRole()));
    }
}
