package inha.gdgoc.global.config.jwt;

import static inha.gdgoc.global.exception.GlobalErrorCode.INVALID_JWT_REQUEST;

import inha.gdgoc.domain.auth.enums.LoginType;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
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

    /**
     * JWT에서 인증 정보를 추출해 Spring Security Authentication 객체를 생성합니다.
     *
     * 토큰의 클레임에서 숫자형 "id"를 읽어 사용자 ID(Long)로 변환하고, 서브젝트를 사용자명(이메일)으로 사용합니다.
     * "role" 클레임을 UserRole로 변환한 뒤 "ROLE_<ROLE_NAME>" 형태의 권한을 하나 생성하여 CustomUserDetails를 만들고
     * 해당 사용자 정보를 담은 UsernamePasswordAuthenticationToken을 반환합니다.
     *
     * @param token JWT 문자열
     * @return 토큰으로부터 생성된 Authentication (principal: CustomUserDetails, credentials: null, authorities 포함)
     * @throws BusinessException token에 숫자형 "id" 클레임이 없을 경우 INVALID_JWT_REQUEST로 발생합니다.
     */
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);

        Number idNum = claims.get("id", Number.class);
        if (idNum == null) throw new BusinessException(INVALID_JWT_REQUEST);
        Long userId = idNum.longValue();

        String username = claims.getSubject();

        UserRole userRole = UserRole.valueOf(claims.get("role", String.class));
        String roleName = "ROLE_" + userRole.name();
        Set<SimpleGrantedAuthority> authorities = Collections.singleton(
                new SimpleGrantedAuthority(roleName)
        );

        CustomUserDetails userDetails = new CustomUserDetails(userId, username, "", authorities);
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
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