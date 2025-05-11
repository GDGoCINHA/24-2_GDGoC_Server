package inha.gdgoc.config.jwt;

import inha.gdgoc.domain.auth.enums.LoginType;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class TokenProvider {
    private final JwtProperties jwtProperties;

    // 자체 로그인용 토큰 생성
    public String generateSelfSignupToken(User user, Duration expiredAt) {
        Date now = new Date();
        return makeToken(
                new Date(now.getTime() + expiredAt.toMillis()),
                user,
                LoginType.SELF_SIGNUP
        );
    }

    // 구글 로그인용 토큰 생성
    public String generateGoogleLoginToken(User user, Duration expiredAt) {
        Date now = new Date();
        return makeToken(
                new Date(now.getTime() + expiredAt.toMillis()),
                user,
                LoginType.GOOGLE_LOGIN
        );
    }

    public String generateRefreshToken(User user, Duration expiredAt, LoginType loginType) {
        Date now = new Date();
        return makeToken(
                new Date(now.getTime() + expiredAt.toMillis()),
                user,
                loginType
        );
    }

    public Claims validToken(String token) throws ExpiredJwtException, UnsupportedJwtException,
            MalformedJwtException, SignatureException, IllegalArgumentException {
        return getClaims(token);
    }

    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        UserRole userRole = UserRole.valueOf(claims.get("role", String.class));
        Long userId = claims.get("id", Integer.class).longValue();
        String username = claims.getSubject();


        Set<SimpleGrantedAuthority> authorities = Collections.singleton(
                new SimpleGrantedAuthority(userRole.getRole())
        );

        CustomUserDetails userDetails = new CustomUserDetails(userId, username, "", authorities);
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                token,
                authorities
        );
    }

    private String makeToken(Date expiry, User user, LoginType loginType) {
        Date now = new Date();
        String issuer = loginType == LoginType.SELF_SIGNUP
                ? jwtProperties.getSelfIssuer()
                : jwtProperties.getGoogleIssuer();

        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .setSubject(user.getEmail())
                .claim("id", user.getId())
                .claim("loginType", loginType.name())
                .claim("role", user.getUserRole().name())
                .signWith(SignatureAlgorithm.HS256,
                        Base64.getEncoder().encodeToString(
                                jwtProperties.getSecretKey().getBytes()
                        )
                )
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(
                        Base64.getEncoder().encodeToString(
                                jwtProperties.getSecretKey().getBytes()
                        )
                )
                .parseClaimsJws(token)
                .getBody();
    }

    @Getter
    public static class CustomUserDetails extends org.springframework.security.core.userdetails.User {
        private final Long userId;

        public CustomUserDetails(Long userId, String username, String password, Collection<? extends GrantedAuthority> authorities) {
            super(username, password, authorities);
            this.userId = userId;
        }

    }
}