package com.example.wallet_service.config;

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
 * Reads the X-Auth-User-Email, X-Auth-User-Role, and X-Auth-User-Id headers
 * injected by the API Gateway's JwtAuthenticationFilter, and populates the
 * Spring SecurityContext so @PreAuthorize and @AuthenticationPrincipal work.
 *
 * If any header is missing (e.g. direct call bypassing gateway in dev),
 * the request continues unauthenticated and Spring Security's standard
 * 401 kicks in for protected endpoints.
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
            // Build Spring Security authentication from gateway headers
            var authority = new SimpleGrantedAuthority("ROLE_" + role);
            var auth = new UsernamePasswordAuthenticationToken(
                    email,   // principal — used by @AuthenticationPrincipal String email
                    userId,  // credentials — userId as string
                    List.of(authority)
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Gateway auth set: email={} role={} userId={}", email, role, userId);
        }

        filterChain.doFilter(request, response);
    }
}
