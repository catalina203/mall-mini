package com.mallmini.security;

import com.mallmini.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    private final SecretKey key;
    private final long expireMinutes;

    public JwtService(AppProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.expireMinutes = properties.getJwt().getExpireMinutes();
    }

    public String generate(Long subjectId, String role) {
        return build(subjectId, role, null);
    }

    public String generateAdmin(Long subjectId, int tokenVersion) {
        return build(subjectId, ROLE_ADMIN, tokenVersion);
    }

    private String build(Long subjectId, String role, Integer tokenVersion) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(String.valueOf(subjectId))
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireMinutes * 60)));
        if (tokenVersion != null) {
            builder.claim("ver", tokenVersion);
        }
        return builder.signWith(key).compact();
    }

    public Claims parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long expireMinutes() {
        return expireMinutes;
    }
}
