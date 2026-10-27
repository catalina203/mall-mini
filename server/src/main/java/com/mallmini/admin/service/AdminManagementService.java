package com.mallmini.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mallmini.admin.dto.AdminCreateRequest;
import com.mallmini.admin.dto.AdminPasswordResetRequest;
import com.mallmini.admin.dto.AdminUpdateRequest;
import com.mallmini.admin.entity.AdminUser;
import com.mallmini.admin.enums.AdminRole;
import com.mallmini.admin.enums.AdminStatus;
import com.mallmini.admin.mapper.AdminUserMapper;
import com.mallmini.admin.vo.AdminVO;
import com.mallmini.common.api.PageResult;
import com.mallmini.common.exception.BusinessException;
import com.mallmini.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminManagementService {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminOperationLogger operationLogger;

    public PageResult<AdminVO> page(long page, long pageSize, String keyword, String status) {
        long safePage = Math.max(page, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<AdminUser> wrapper = new LambdaQueryWrapper<AdminUser>()
                .orderByAsc(AdminUser::getId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(AdminUser::getStatus, parseStatus(status).name());
        }
        if (keyword != null && !keyword.isBlank()) {
            String normalized = keyword.trim();
            wrapper.and(condition -> condition
                    .like(AdminUser::getUsername, normalized)
                    .or()
                    .like(AdminUser::getDisplayName, normalized));
        }
        Page<AdminUser> result = adminUserMapper.selectPage(
                new Page<>(safePage, safePageSize), wrapper);
        List<AdminVO> items = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(items, safePage, safePageSize, result.getTotal());
    }

    @Transactional
    public AdminVO create(AdminCreateRequest request, Long actorId) {
        String username = request.username().trim();
        if (adminUserMapper.selectCount(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, username)) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
        AdminUser admin = new AdminUser();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setDisplayName(request.displayName() == null || request.displayName().isBlank()
                ? username : request.displayName().trim());
        admin.setRole(parseRole(request.role()).name());
        admin.setStatus(AdminStatus.ACTIVE.name());
        admin.setTokenVersion(0);
        adminUserMapper.insert(admin);
        operationLogger.log(actorId, "ADMIN_CREATE", "ADMIN",
                String.valueOf(admin.getId()), admin.getUsername() + " " + admin.getRole());
        return toVO(admin);
    }

    @Transactional
    public AdminVO update(Long id, AdminUpdateRequest request, Long actorId) {
        AdminUser admin = require(id);
        AdminRole targetRole = parseRole(request.role());
        String currentRole = AdminRole.from(admin.getRole()).name();
        if (id.equals(actorId) && !currentRole.equals(targetRole.name())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "不能修改自己的角色");
        }
        if (AdminRole.SUPER_ADMIN.name().equals(currentRole)
                && !AdminRole.SUPER_ADMIN.name().equals(targetRole.name())) {
            ensureAnotherActiveSuperAdmin(id);
        }
        admin.setDisplayName(request.displayName() == null || request.displayName().isBlank()
                ? admin.getUsername() : request.displayName().trim());
        boolean roleChanged = !currentRole.equals(targetRole.name());
        admin.setRole(targetRole.name());
        if (roleChanged) {
            admin.setTokenVersion(nextVersion(admin));
        }
        adminUserMapper.updateById(admin);
        operationLogger.log(actorId, "ADMIN_UPDATE", "ADMIN",
                String.valueOf(id), admin.getDisplayName() + " " + admin.getRole());
        return toVO(admin);
    }

    @Transactional
    public void updateStatus(Long id, String statusValue, Long actorId) {
        AdminUser admin = require(id);
        AdminStatus target = parseStatus(statusValue);
        if (id.equals(actorId) && target == AdminStatus.DISABLED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "不能禁用自己");
        }
        if (target == AdminStatus.DISABLED
                && AdminRole.SUPER_ADMIN.name().equals(AdminRole.from(admin.getRole()).name())) {
            ensureAnotherActiveSuperAdmin(id);
        }
        String before = admin.getStatus();
        admin.setStatus(target.name());
        admin.setTokenVersion(nextVersion(admin));
        adminUserMapper.updateById(admin);
        operationLogger.log(actorId, "ADMIN_STATUS", "ADMIN",
                String.valueOf(id), before + "->" + target.name());
    }

    @Transactional
    public void resetPassword(Long id, AdminPasswordResetRequest request, Long actorId) {
        AdminUser admin = require(id);
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setTokenVersion(nextVersion(admin));
        adminUserMapper.updateById(admin);
        operationLogger.log(actorId, "ADMIN_PASSWORD_RESET", "ADMIN",
                String.valueOf(id), admin.getUsername());
    }

    @Transactional
    public void changePassword(Long adminId, String oldPassword, String newPassword) {
        AdminUser admin = require(adminId);
        if (!passwordEncoder.matches(oldPassword, admin.getPasswordHash())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "原密码不正确");
        }
        if (oldPassword.equals(newPassword)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "新密码不能与原密码相同");
        }
        admin.setPasswordHash(passwordEncoder.encode(newPassword));
        admin.setTokenVersion(nextVersion(admin));
        adminUserMapper.updateById(admin);
        operationLogger.log(adminId, "ADMIN_PASSWORD_CHANGE", "ADMIN",
                String.valueOf(adminId), "修改自己的密码");
    }

    public List<String> roles() {
        return AdminRole.options();
    }

    private AdminUser require(Long id) {
        AdminUser admin = adminUserMapper.selectById(id);
        if (admin == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "管理员不存在");
        }
        return admin;
    }

    private void ensureAnotherActiveSuperAdmin(Long excludeId) {
        long count = adminUserMapper.selectCount(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getRole, AdminRole.SUPER_ADMIN.name())
                .eq(AdminUser::getStatus, AdminStatus.ACTIVE.name())
                .ne(AdminUser::getId, excludeId));
        if (count == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "必须至少保留一个启用的超级管理员");
        }
    }

    private int nextVersion(AdminUser admin) {
        return (admin.getTokenVersion() == null ? 0 : admin.getTokenVersion()) + 1;
    }

    private AdminRole parseRole(String value) {
        try {
            return AdminRole.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "管理员角色不合法");
        }
    }

    private AdminStatus parseStatus(String value) {
        try {
            return AdminStatus.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "管理员状态不合法");
        }
    }

    private AdminVO toVO(AdminUser admin) {
        return new AdminVO(
                admin.getId(),
                admin.getUsername(),
                admin.getDisplayName(),
                AdminRole.from(admin.getRole()).name(),
                admin.getStatus(),
                AdminPermissionService.permissionCodes(admin.getRole()));
    }
}
