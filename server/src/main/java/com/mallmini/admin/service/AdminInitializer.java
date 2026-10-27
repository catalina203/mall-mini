package com.mallmini.admin.service;

import com.mallmini.admin.entity.AdminUser;
import com.mallmini.admin.enums.AdminRole;
import com.mallmini.admin.enums.AdminStatus;
import com.mallmini.admin.mapper.AdminUserMapper;
import com.mallmini.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        Long count = adminUserMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }

        String password = properties.getAdminInit().getPassword();
        if (password == null || password.isBlank()) {
            log.warn("no admin account exists; set APP_ADMIN_INIT_USERNAME/APP_ADMIN_INIT_PASSWORD to initialize one");
            return;
        }
        if (password.length() < 8) {
            log.warn("APP_ADMIN_INIT_PASSWORD must be at least 8 characters; admin account not created");
            return;
        }

        String username = properties.getAdminInit().getUsername();
        AdminUser admin = new AdminUser();
        admin.setUsername(username == null || username.isBlank() ? "admin" : username.trim());
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setDisplayName("管理员");
        admin.setRole(AdminRole.SUPER_ADMIN.name());
        admin.setStatus(AdminStatus.ACTIVE.name());
        admin.setTokenVersion(0);
        adminUserMapper.insert(admin);
        log.info("admin account initialized: {}", admin.getUsername());
    }
}
