package com.mallmini.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class AppStartupValidator {

    private static final Logger log = LoggerFactory.getLogger(AppStartupValidator.class);
    private static final int MIN_SECRET_LENGTH = 32;
    private static final String DEV_DEFAULT_SECRET = "dev-only-change-this-secret-at-least-32-bytes-long";

    private final AppProperties properties;
    private final Environment environment;

    public AppStartupValidator(AppProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        String secret = properties.getJwt().getSecret();
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT_SECRET 长度必须不少于 " + MIN_SECRET_LENGTH + " 个字符");
        }

        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            if (DEV_DEFAULT_SECRET.equals(secret)) {
                throw new IllegalStateException("生产环境必须通过 JWT_SECRET 设置独立密钥，不能使用开发默认值");
            }
            if (properties.isDevLoginEnabled()) {
                throw new IllegalStateException("生产环境禁止开启开发登录（APP_DEV_LOGIN_ENABLED）");
            }
            if (properties.getPayment().isMockEnabled()) {
                throw new IllegalStateException("生产环境禁止开启模拟支付（APP_PAYMENT_MOCK_ENABLED）");
            }
            log.info("production configuration validated");
        }
    }
}
