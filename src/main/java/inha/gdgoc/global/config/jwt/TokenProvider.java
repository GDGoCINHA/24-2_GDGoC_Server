package inha.gdgoc.global.config.jwt;

import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.crypto.SecretKey;

import static inha.gdgoc.global.exception.GlobalErrorCode.INVALID_JWT_REQUEST;

@RequiredArgsConstructor
@Service
public class TokenProvider {

    private static final long ALLOWED_CLOCK_SKEW_SECONDS = 5L;

    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_SESSION_ID = "sid";
    private SecretKey cachedSigningKey;

    @PostConstruct
    void initSigningKey() {
        this.cachedSigningKey = buildSigningKey(jwtProperties.getSecretKey());
    }

    // Access Token 생성 (JWT)
    public String createAccessToken(User user, String sessionId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + jwtProperties.getAccessTokenValidity());

        var builder = Jwts.builder()
                .issuer(jwtProperties.getSelfIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .issuedAt(now)
                .expiration(validity)
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_SESSION_ID, sessionId);

        return builder
                .signWith(signingKey())
                .compact();
    }


    // Refresh Token 생성 (Random UUID)
    // JWT가 아니라, 단순 랜덤 문자열로 생성하여 Redis 저장용으로 씁니다.
    public String createRefreshToken() {
        return UUID.randomUUID().toString();
    }

    // Authentication 객체 생성 (Spring Security용)
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);

        Long userId = extractUserId(claims);
        String sessionId = claims.get(CLAIM_SESSION_ID, String.class);
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("JWT 검증 실패: sessionId(sid) 클레임이 누락되었습니다.");
            throw new BusinessException(INVALID_JWT_REQUEST);
        }

        validateAudienceClaim(claims.get(Claims.AUDIENCE));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("JWT 검증 실패: ID가 {}인 유저를 찾을 수 없습니다.", userId);
                    return new BusinessException(INVALID_JWT_REQUEST);
                });

        UserRole userRole = user.getUserRole();
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + userRole.name()));

        TeamType team = user.getTeam();
        if (team != null) {
            authorities.add(new SimpleGrantedAuthority("TEAM_" + team.name()));
        }

        CustomUserDetails userDetails =
                new CustomUserDetails(userId, user.getEmail(), sessionId, authorities, userRole, team);

        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }

    private Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .clockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS)
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT 검증 실패: 만료된 토큰입니다.");
            throw e;
        } catch (UnsupportedJwtException e) {
            log.warn("JWT 검증 실패: 지원되지 않는 토큰 형식입니다.");
            throw e;
        } catch (MalformedJwtException e) {
            log.warn("JWT 검증 실패: 잘못된 구조의 토큰입니다.");
            throw e;
        } catch (SignatureException e) {
            log.warn("JWT 검증 실패: 서명이 일치하지 않습니다.");
            throw e;
        } catch (Exception e) {
            log.warn("JWT 검증 실패: 알 수 없는 오류 발생 - {}", e.getMessage());
            throw e;
        }
    }

    private SecretKey signingKey() {
        return cachedSigningKey;
    }

    private SecretKey buildSigningKey(String rawSecret) {
        byte[] candidateKey;
        try {
            candidateKey = Decoders.BASE64.decode(rawSecret);
        } catch (IllegalArgumentException ignore) {
            candidateKey = rawSecret.getBytes(StandardCharsets.UTF_8);
        }

        if (candidateKey.length < 32) {
            try {
                candidateKey = MessageDigest.getInstance("SHA-256").digest(candidateKey);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 algorithm not available", e);
            }
        }

        return Keys.hmacShaKeyFor(candidateKey);
    }

    private Long extractUserId(Claims claims) {
        Number idNum = claims.get(CLAIM_USER_ID, Number.class);
        if (idNum == null) {
            log.warn("JWT 검증 실패: userId(uid) 클레임이 누락되었습니다.");
            throw new BusinessException(INVALID_JWT_REQUEST);
        }
        return idNum.longValue();
    }

    private void validateAudienceClaim(Object audienceClaim) {
        if (audienceClaim == null) {
            log.warn("JWT 검증 실패: audience(aud) 클레임이 누락되었습니다.");
            throw new BusinessException(INVALID_JWT_REQUEST);
        }

        String expectedAudience = jwtProperties.getAudience();
        if (audienceClaim instanceof Collection<?> collection) {
            boolean matches = collection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .anyMatch(expectedAudience::equals);
            if (!matches) {
                log.warn("JWT 검증 실패: audience 불일치. (기대값: {}, 실제값: {})", expectedAudience, collection);
                throw new BusinessException(INVALID_JWT_REQUEST);
            }
            return;
        }

        if (!expectedAudience.equals(audienceClaim.toString())) {
            log.warn("JWT 검증 실패: audience 불일치. (기대값: {}, 실제값: {})", expectedAudience, audienceClaim);
            throw new BusinessException(INVALID_JWT_REQUEST);
        }
    }

    @Getter
    public static class CustomUserDetails extends org.springframework.security.core.userdetails.User {

        private final Long userId;
        private final String sessionId;
        private final UserRole role;
        private final TeamType team;

        public CustomUserDetails(
                Long userId,
                String username,
                String sessionId,
                Collection<? extends GrantedAuthority> authorities,
                UserRole role,
                TeamType team
        ) {
            super(username, "", authorities);
            this.userId = userId;
            this.sessionId = sessionId;
            this.role = role;
            this.team = team;
        }
    }
}
