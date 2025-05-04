package inha.gdgoc.config;

import inha.gdgoc.config.jwt.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
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
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        List<String> skipPaths = List.of("/auth/refresh", "/auth/login", "/auth/oauth2/google/callback",
                "/auth/signup", "/auth/findId");
        if (skipPaths.contains(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = getAccessToken(request);
        log.info("요청 URI: {}, 추출된 access token: {}", request.getRequestURI(), token);

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
            log.info("Authorization 헤더 없음 → 인증 시도 안함");
        }

        filterChain.doFilter(request, response);
    }

    private String getAccessToken(HttpServletRequest request) {
        final String HEADER_AUTHORIZATION = "Authorization";
        final String TOKEN_PREFIX = "Bearer ";

        String bearerToken = request.getHeader(HEADER_AUTHORIZATION);

        if (bearerToken == null || !bearerToken.startsWith(TOKEN_PREFIX)) {
            return null;
        }

        String token = bearerToken.substring(TOKEN_PREFIX.length());

        token = token.trim();

        for (char c : token.toCharArray()) {
            if (c < 32) {
                log.info("토큰에 유효하지 않은 제어 문자가 포함되어 있습니다.");
                throw new IllegalArgumentException("토큰에 유효하지 않은 제어 문자가 포함되어 있습니다.");
            }
        }

        return token;
    }

}
