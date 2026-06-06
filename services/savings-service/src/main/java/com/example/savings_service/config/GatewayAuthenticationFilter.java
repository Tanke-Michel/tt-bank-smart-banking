package com.example.savings_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads X-Auth-* headers injected by the API Gateway and populates
 * Spring SecurityContext so @PreAuthorize and @AuthenticationPrincipal work.
 */
@Slf4j
@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_EMAIL  = "X-Auth-User-Email";
    public static final String HEADER_ROLE   = "X-Auth-User-Role";
    public static final String HEADER_USERID = "X-Auth-User-Id";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String email  = request.getHeader(HEADER_EMAIL);
        String role   = request.getHeader(HEADER_ROLE);
        String userId = request.getHeader(HEADER_USERID);

        if (StringUtils.hasText(email) && StringUtils.hasText(role)) {
            var authority = new SimpleGrantedAuthority("ROLE_" + role);
            var auth = new UsernamePasswordAuthenticationToken(
                    email, userId, List.of(authority));
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Gateway auth: email={} role={} userId={}", email, role, userId);
        }

        filterChain.doFilter(request, response);
    }
}
