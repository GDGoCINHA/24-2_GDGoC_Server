package inha.gdgoc.global.config.jwt;

import inha.gdgoc.domain.auth.enums.LoginType;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.exception.BusinessException;
import io.jsonwebtoken.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

import static inha.gdgoc.global.exception.GlobalErrorCode.INVALID_JWT_REQUEST;

@RequiredArgsConstructor
@Service
public class TokenProvider {

    private final JwtProperties jwtProperties;

    // Access Token 생성 (JWT)
    public String createAccessToken(User user){
        Date now = new Date();
        Date validity = new Date(now.getTime() + jwtProperties.getAccessTokenValidity()); // application.properties에서 시간 가져옴

    String teamName = (user.getTeam() != null) ? user.getTeam().name() : null;

    return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuer(jwtProperties.getGoogleIssuer()) // Issuer는 하나로 통일 (또는 제거 가능)
                .setIssuedAt(now)
                .setExpiration(validity)
                .setSubject(user.getEmail())          // sub: 이메일
                .claim("id", user.getId())            // claim: 유저 PK (DB 조회용)
                .claim("role", user.getUserRole().name()) // claim: 권한
                .claim("team", teamName)              // claim: 팀 (없으면 null)
                .signWith(SignatureAlgorithm.HS256, Base64.getEncoder()
                        .encodeToString(jwtProperties.getSecretKey().getBytes()))
                .compact();
    }


    // Refresh Token 생성 (Random UUID)
    // JWT가 아니라, 단순 랜덤 문자열로 생성하여 Redis 저장용으로 씁니다.
    public String createRefreshToken() {
        return UUID.randomUUID().toString();
    }

    // 토큰 유효성 검증
    public Claims validToken(String token) throws ExpiredJwtException, UnsupportedJwtException, MalformedJwtException, SignatureException, IllegalArgumentException {
        return getClaims(token);
    }

    // Authentication 객체 생성 (Spring Security용)
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);

        // ID 추출
        Number idNum = claims.get("id", Number.class);
        if (idNum == null) throw new BusinessException(INVALID_JWT_REQUEST);
        Long userId = idNum.longValue();

        String email = claims.getSubject();

        // role (필수)
        String roleStr = claims.get("role", String.class);
        if (roleStr == null) throw new BusinessException(INVALID_JWT_REQUEST);
        UserRole userRole = UserRole.valueOf(roleStr);

        // 권한 세트 구성
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        // 1) 역할 권한
        authorities.add(new SimpleGrantedAuthority("ROLE_" + userRole.name()));

        // 2) 팀 권한 (선택)
        TeamType team = null;
        String teamStr = claims.get("team", String.class);
        if (teamStr != null && !teamStr.isBlank()) {
            try {
                team = TeamType.valueOf(teamStr);
                authorities.add(new SimpleGrantedAuthority("TEAM_" + team.name()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        CustomUserDetails userDetails = new CustomUserDetails(userId, email, "", authorities, userRole, team);

        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }
    private Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(Base64.getEncoder().encodeToString(jwtProperties.getSecretKey().getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Getter
    public static class CustomUserDetails extends org.springframework.security.core.userdetails.User {

        private final Long userId;
        private final UserRole role;
        private final TeamType team;

        public CustomUserDetails(Long userId, String username, String password, Collection<? extends GrantedAuthority> authorities, UserRole role, TeamType team) {
            super(username, password, authorities);
            this.userId = userId;
            this.role = role;
            this.team = team;
        }
    }
}
