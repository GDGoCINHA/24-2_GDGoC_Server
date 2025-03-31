package inha.gdgoc.config;

import inha.gdgoc.config.jwt.TokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final TokenProvider tokenProvider;

    private final String HEADER_AUTHORIZATION = "Authorization";
    private final String TOKEN_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
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
        String bearerToken = request.getHeader(HEADER_AUTHORIZATION);
        return (bearerToken != null && bearerToken.startsWith(TOKEN_PREFIX)) ? bearerToken.substring(7) : null;
    }
}
