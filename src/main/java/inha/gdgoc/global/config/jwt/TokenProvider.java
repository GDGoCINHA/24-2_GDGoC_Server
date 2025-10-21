package inha.gdgoc.global.config.jwt;

import static inha.gdgoc.global.exception.GlobalErrorCode.INVALID_JWT_REQUEST;

import inha.gdgoc.domain.auth.enums.LoginType;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.exception.BusinessException;
import io.jsonwebtoken.*;
import java.time.Duration;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class TokenProvider {
    private final JwtProperties jwtProperties;

    // 자체 로그인용 토큰 생성
    public String generateSelfSignupToken(User user, Duration expiredAt) {
        Date now = new Date();
        return makeToken(new Date(now.getTime() + expiredAt.toMillis()), user, LoginType.SELF_SIGNUP);
    }

    // 구글 로그인용 토큰 생성
    public String generateGoogleLoginToken(User user, Duration expiredAt) {
        Date now = new Date();
        return makeToken(new Date(now.getTime() + expiredAt.toMillis()), user, LoginType.GOOGLE_LOGIN);
    }

    public String generateRefreshToken(User user, Duration expiredAt, LoginType loginType) {
        Date now = new Date();
        return makeToken(new Date(now.getTime() + expiredAt.toMillis()), user, loginType);
    }

    public Claims validToken(String token) throws ExpiredJwtException, UnsupportedJwtException,
            MalformedJwtException, SignatureException, IllegalArgumentException {
        return getClaims(token);
    }

    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);

        Number idNum = claims.get("id", Number.class);
        if (idNum == null) throw new BusinessException(INVALID_JWT_REQUEST);
        Long userId = idNum.longValue();

        String username = claims.getSubject();

        // role (필수)
        String roleStr = claims.get("role", String.class);
        if (roleStr == null) throw new BusinessException(INVALID_JWT_REQUEST);
        UserRole userRole = UserRole.valueOf(roleStr);
        String roleName = "ROLE_" + userRole.name();
        Set<SimpleGrantedAuthority> authorities =
                Collections.singleton(new SimpleGrantedAuthority(roleName));

        // team (선택) - 토큰에는 enum name(String)으로 저장됨. null/오타는 무시.
        TeamType team = null;
        String teamStr = claims.get("team", String.class);
        if (teamStr != null && !teamStr.isBlank()) {
            try {
                team = TeamType.valueOf(teamStr);
            } catch (IllegalArgumentException ignored) {
                // 구버전 토큰 또는 잘못된 값이면 null 유지
            }
        }

        CustomUserDetails userDetails =
                new CustomUserDetails(userId, username, "", authorities, userRole, team);

        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }

    private String makeToken(Date expiry, User user, LoginType loginType) {
        Date now = new Date();
        String issuer = (loginType == LoginType.SELF_SIGNUP)
                ? jwtProperties.getSelfIssuer()
                : jwtProperties.getGoogleIssuer();

        // team: enum name 저장(예: "PR_DESIGN"), 없으면 null
        String teamEnumName = (user.getTeam() == null) ? null : user.getTeam().name();

        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .setSubject(user.getEmail())
                .claim("id", user.getId())
                .claim("loginType", loginType.name())
                .claim("role", user.getUserRole().name())
                .claim("team", teamEnumName)
                .signWith(SignatureAlgorithm.HS256,
                        Base64.getEncoder().encodeToString(jwtProperties.getSecretKey().getBytes()))
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(Base64.getEncoder().encodeToString(jwtProperties.getSecretKey().getBytes()))
                .parseClaimsJws(token)
                .getBody();
    }

    @Getter
    public static class CustomUserDetails extends org.springframework.security.core.userdetails.User {
        private final Long userId;
        private final UserRole role;
        private final TeamType team;

        public CustomUserDetails(Long userId,
                                 String username,
                                 String password,
                                 Collection<? extends GrantedAuthority> authorities,
                                 UserRole role,
                                 TeamType team) {
            super(username, password, authorities);
            this.userId = userId;
            this.role = role;
            this.team = team;
        }
    }
}