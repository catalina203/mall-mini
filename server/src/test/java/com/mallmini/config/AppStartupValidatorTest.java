package com.mallmini.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppStartupValidatorTest {

    private static final String VALID_SECRET = "0123456789012345678901234567890123456789";

    @Test
    void rejectsShortSecret() {
        AppProperties properties = new AppProperties();
        properties.getJwt().setSecret("too-short");

        AppStartupValidator validator = new AppStartupValidator(properties, new MockEnvironment());

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void rejectsDefaultSecretInProd() {
        AppProperties properties = new AppProperties();
        properties.getJwt().setSecret("dev-only-change-this-secret-at-least-32-bytes-long");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        AppStartupValidator validator = new AppStartupValidator(properties, environment);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void rejectsDevLoginInProd() {
        AppProperties properties = new AppProperties();
        properties.getJwt().setSecret(VALID_SECRET);
        properties.setDevLoginEnabled(true);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        AppStartupValidator validator = new AppStartupValidator(properties, environment);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("开发登录");
    }

    @Test
    void rejectsMockPaymentInProd() {
        AppProperties properties = new AppProperties();
        properties.getJwt().setSecret(VALID_SECRET);
        properties.getPayment().setMockEnabled(true);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        AppStartupValidator validator = new AppStartupValidator(properties, environment);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("模拟支付");
    }

    @Test
    void acceptsDevProfile() {
        AppProperties properties = new AppProperties();
        properties.getJwt().setSecret(VALID_SECRET);
        properties.setDevLoginEnabled(true);
        properties.getPayment().setMockEnabled(true);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        AppStartupValidator validator = new AppStartupValidator(properties, environment);

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }
}
