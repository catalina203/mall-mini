package com.mallmini.security;

import com.mallmini.admin.entity.AdminUser;
import com.mallmini.admin.enums.AdminStatus;
import com.mallmini.admin.mapper.AdminUserMapper;
import com.mallmini.user.entity.User;
import com.mallmini.user.enums.UserStatus;
import com.mallmini.user.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final AdminUserMapper adminUserMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.parse(header.substring(PREFIX.length()));
            Long subjectId = Long.valueOf(claims.getSubject());
            String role = claims.get("role", String.class);
            if (role == null) {
                filterChain.doFilter(request, response);
                return;
            }

            if (JwtService.ROLE_USER.equals(role) && !isActiveUser(subjectId)) {
                filterChain.doFilter(request, response);
                return;
            }
            if (JwtService.ROLE_ADMIN.equals(role) && !isActiveAdmin(subjectId)) {
                filterChain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    new AuthUser(subjectId, role),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            log.debug("invalid jwt: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private boolean isActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && UserStatus.ACTIVE.name().equals(user.getStatus());
    }

    private boolean isActiveAdmin(Long adminId) {
        AdminUser admin = adminUserMapper.selectById(adminId);
        return admin != null && AdminStatus.ACTIVE.name().equals(admin.getStatus());
    }
}
