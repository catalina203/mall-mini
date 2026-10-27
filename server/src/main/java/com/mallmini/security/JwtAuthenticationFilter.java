package com.mallmini.security;

import com.mallmini.admin.entity.AdminUser;
import com.mallmini.admin.enums.AdminPermission;
import com.mallmini.admin.enums.AdminStatus;
import com.mallmini.admin.mapper.AdminUserMapper;
import com.mallmini.admin.service.AdminPermissionService;
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
import java.util.ArrayList;
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

            if (JwtService.ROLE_USER.equals(role)) {
                if (!isActiveUser(subjectId)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        new AuthUser(subjectId, role),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else if (JwtService.ROLE_ADMIN.equals(role)) {
                AdminUser admin = adminUserMapper.selectById(subjectId);
                if (admin == null || !AdminStatus.ACTIVE.name().equals(admin.getStatus())) {
                    filterChain.doFilter(request, response);
                    return;
                }
                Integer tokenVersion = claims.get("ver", Integer.class);
                int currentVersion = admin.getTokenVersion() == null ? 0 : admin.getTokenVersion();
                if (tokenVersion == null || tokenVersion != currentVersion) {
                    filterChain.doFilter(request, response);
                    return;
                }
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("ROLE_" + admin.getRole()));
                for (AdminPermission permission : AdminPermissionService.permissionsOf(admin.getRole())) {
                    authorities.add(new SimpleGrantedAuthority(permission.name()));
                }
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        new AuthUser(subjectId, admin.getRole()),
                        null,
                        authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
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


}
