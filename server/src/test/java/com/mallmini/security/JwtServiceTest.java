package com.mallmini.security;

import com.mallmini.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "0123456789012345678901234567890123456789";

    @Test
    void generatesAndParsesToken() {
        JwtService jwtService = new JwtService(properties());

        String token = jwtService.generate(42L, JwtService.ROLE_USER);
        Claims claims = jwtService.parse(token);

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("role", String.class)).isEqualTo(JwtService.ROLE_USER);
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void rejectsTamperedToken() {
        JwtService jwtService = new JwtService(properties());
        String token = jwtService.generate(1L, JwtService.ROLE_USER) + "x";

        assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(JwtException.class);
    }

    private AppProperties properties() {
        AppProperties properties = new AppProperties();
        properties.getJwt().setSecret(SECRET);
        properties.getJwt().setExpireMinutes(60);
        return properties;
    }
}
