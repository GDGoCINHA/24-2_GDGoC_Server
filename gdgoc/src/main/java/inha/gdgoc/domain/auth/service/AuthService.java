package inha.gdgoc.domain.auth.service;

import inha.gdgoc.config.jwt.TokenProvider;
import inha.gdgoc.domain.auth.dto.request.UserLoginRequest;
import inha.gdgoc.domain.auth.dto.response.LoginResponse;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

import static inha.gdgoc.util.EncryptUtil.encrypt;

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
                    "exists", false,
                    "email", email,
                    "name", name
            );
        }

        User user = foundUser.get();

        String jwtAccessToken = tokenProvider.generateGoogleLoginToken(user, Duration.ofHours(1));
        String refreshToken = refreshTokenService.getOrCreateRefreshToken(user, Duration.ofDays(1));

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .domain(".gdgocinha.site")
                .maxAge(Duration.ofDays(1))
                .build();

        // Set-Cookie 헤더로 추가
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return Map.of(
                "exists", true,
                "access_token", jwtAccessToken
        );
    }

    public LoginResponse loginWithPassword(UserLoginRequest userLoginRequest, HttpServletResponse response) {
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
        String refreshToken = refreshTokenService.getOrCreateRefreshToken(foundUser, Duration.ofDays(1));

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .domain(".gdgocinha.site")
                .maxAge(Duration.ofDays(1))
                .build();

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
