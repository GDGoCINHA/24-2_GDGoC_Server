package inha.gdgoc.domain.auth.service;

import inha.gdgoc.global.config.jwt.TokenProvider;
import inha.gdgoc.domain.auth.dto.request.UserLoginRequest;
import inha.gdgoc.domain.auth.dto.response.LoginResponse;
import inha.gdgoc.domain.auth.enums.LoginType;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static inha.gdgoc.global.util.EncryptUtil.encrypt;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RefreshTokenService refreshTokenService;

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final TokenProvider tokenProvider;

    /**
     * Google OAuth 인증 코드를 처리하여 사용자 정보 확인 및 로그인 토큰을 발급한다.
     *
     * <p>작업 흐름:
     * <ol>
     *   <li>전달된 authorization code로 Google 토큰 엔드포인트에 요청하여 Google access token을 획득한다.</li>
     *   <li>획득한 Google access token으로 Google 사용자정보(email, name)를 조회한다.</li>
     *   <li>조회한 이메일로 로컬 사용자 조회:
     *     <ul>
     *       <li>사용자가 존재하지 않으면 사용자 존재 여부와 Google에서의 email, name을 반환한다.</li>
     *       <li>사용자가 존재하면 서비스용 JWT access token을 생성하고(유효기간 1시간),/ 로그인용 refresh token을 생성 또는 조회(유효기간 1일)하여
     *           HttpOnly Secure 쿠키("refresh_token", SameSite=None, domain=".gdgocinha.com", path="/")로 응답에 설정한 뒤 access token을 반환한다.</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * @param code Google이 발급한 authorization code
     * @param response 존재하는 사용자일 때 refresh_token 쿠키를 HttpServletResponse의 Set-Cookie 헤더로 추가하기 위해 사용되는 응답 객체
     * @return 결과 맵:
     *         <ul>
     *           <li>사용자가 존재하지 않을 때: {"isExists": false, "email": &lt;google 이메일&gt;, "name": &lt;google 이름&gt;}</li>
     *           <li>사용자가 존재할 때: {"isExists": true, "access_token": &lt;서비스용 JWT access token&gt;}</li>
     *         </ul>
     */
    public Map<String, Object> processOAuthLogin(String code, HttpServletResponse response) {
        // 1. code → access token 요청
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(params, headers);
        ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
            "https://oauth2.googleapis.com/token",
            tokenRequest,
            Map.class
        );

        String googleAccessToken = (String) tokenResponse.getBody().get("access_token");

        // 2. access token → 사용자 정보 요청
        HttpHeaders userInfoHeaders = new HttpHeaders();
        userInfoHeaders.setBearerAuth(googleAccessToken);
        HttpEntity<Void> userInfoRequest = new HttpEntity<>(userInfoHeaders);

        ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
            "https://www.googleapis.com/oauth2/v2/userinfo",
            HttpMethod.GET,
            userInfoRequest,
            Map.class
        );

        // 3. Google에서 가져온 이름, 이메일로 가입된 정보가 없으면 회원가입, 있으면 로그인
        Map userInfo = userInfoResponse.getBody();
        String email = (String) userInfo.get("email");
        String name = (String) userInfo.get("name");

        Optional<User> foundUser = userRepository.findByEmail(email);
        if (foundUser.isEmpty()) {
            return Map.of(
                "isExists", false,
                "email", email,
                "name", name
            );
        }

        User user = foundUser.get();

        String jwtAccessToken = tokenProvider.generateGoogleLoginToken(user, Duration.ofHours(1));
        String refreshToken = refreshTokenService.getOrCreateRefreshToken(user, Duration.ofDays(1),
            LoginType.GOOGLE_LOGIN);

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .domain(".gdgocinha.com")
            .path("/")
            .maxAge(Duration.ofDays(1))
            .build();

        // Set-Cookie 헤더로 추가
        log.info("Response Cookie에 저장된 Refresh Token: {}", refreshCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return Map.of(
            "isExists", true,
            "access_token", jwtAccessToken
        );
    }

    public LoginResponse loginWithPassword(UserLoginRequest userLoginRequest,
        HttpServletResponse response)
        throws NoSuchAlgorithmException, InvalidKeyException {
        Optional<User> user = userRepository.findByEmail(userLoginRequest.email());
        if (user.isEmpty()) {
            return new LoginResponse(false, null);
        }

        User foundUser = user.get();
        String hashedInputPassword = encrypt(userLoginRequest.password(), foundUser.getSalt());
        if (!foundUser.getPassword().equals(hashedInputPassword)) {
            return new LoginResponse(false, null);
        }

        String accessToken = tokenProvider.generateSelfSignupToken(foundUser, Duration.ofHours(1));
        String refreshToken = refreshTokenService.getOrCreateRefreshToken(foundUser,
            Duration.ofDays(1),
            LoginType.SELF_SIGNUP);

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .path("/")
            .maxAge(Duration.ofDays(1))
            .build();

        log.info("Response Cookie에 저장된 Refresh Token: {}", refreshCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return new LoginResponse(true, accessToken);
    }

    public Long getAuthenticationUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof TokenProvider.CustomUserDetails user) {
            return user.getUserId();
        }
        throw new IllegalArgumentException("user Id is null");
    }
}
