package inha.gdgoc.global.security;

import inha.gdgoc.global.config.jwt.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        } 

        String uri = request.getRequestURI();

        if (uri.equals("/api/v1/auth/logout")) return false;

        return uri.startsWith("/v3/api-docs")
            || uri.startsWith("/swagger-ui")
            || uri.equals("/swagger-ui.html")
            || uri.startsWith("/api/v1/auth/refresh")
            || uri.startsWith("/api/v1/auth/login")
            || uri.startsWith("/api/v1/auth/oauth2/google/callback")
            || uri.startsWith("/api/v1/auth/password-reset/request")
            || uri.startsWith("/api/v1/auth/password-reset/verify")
            || uri.startsWith("/api/v1/auth/password-reset/confirm")
            || uri.startsWith("/api/v1/test/")
            || uri.startsWith("/api/v1/game/")
            || uri.startsWith("/api/v1/apply/")
            || uri.startsWith("/api/v1/check/");
    }

    @Override
    protected void doFilterInternal(
        @NotNull HttpServletRequest request,
        @NotNull HttpServletResponse response,
        @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        String token = getAccessToken(request);
        log.info("요청 URI: {}, access token 존재 여부: {}", request.getRequestURI(), token != null);

        if (token != null) {
            try {
                Authentication authentication = tokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("JWT 인증 성공: {}", authentication.getName());
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                log.warn("JWT 인증 실패: {}", e.getMessage());
            }
        } else {
            log.info("access token 없음 → 인증 시도 안함");
        }

        filterChain.doFilter(request, response);
    }

    private String getAccessToken(HttpServletRequest request) {
        final String HEADER_AUTHORIZATION = "Authorization";
        final String TOKEN_PREFIX = "Bearer ";

        String authorizationHeader = request.getHeader(HEADER_AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith(TOKEN_PREFIX)) {
            return sanitizeToken(authorizationHeader.substring(TOKEN_PREFIX.length()).trim());
        }

        return null;
    }

    private String readCookieToken(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String sanitizeToken(String token) {
        for (char c : token.toCharArray()) {
            if (c < 32) {
                log.info("토큰에 유효하지 않은 제어 문자가 포함되어 있습니다.");
                throw new IllegalArgumentException("토큰에 유효하지 않은 제어 문자가 포함되어 있습니다.");
            }
        }
        return token;
    }

}
