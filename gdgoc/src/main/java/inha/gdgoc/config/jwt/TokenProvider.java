package inha.gdgoc.config.jwt;

import inha.gdgoc.domain.auth.enums.LoginType;
import inha.gdgoc.domain.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

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

    public boolean validToken(String token, LoginType expectedLoginType) {
        try {
            Claims claims = getClaims(token);

            // 로그인 타입 검증 추가
            LoginType tokenLoginType = LoginType.valueOf(
                    claims.get("loginType", String.class)
            );

            return tokenLoginType == expectedLoginType;
        } catch (Exception e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        LoginType loginType = LoginType.valueOf(
                claims.get("loginType", String.class)
        );

        Set<SimpleGrantedAuthority> authorities = Collections.singleton(
                new SimpleGrantedAuthority("Guest")
        );

        return new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        claims.getSubject(),
                        "",
                        authorities
                ),
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
                .claim("role", user.getUserRole().getRole())
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
}
