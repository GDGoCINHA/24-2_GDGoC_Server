package inha.gdgoc.domain.auth.service;

import inha.gdgoc.config.jwt.TokenProvider;
import inha.gdgoc.domain.user.entity.User;
import jakarta.servlet.http.Cookie;
import java.util.Optional;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import inha.gdgoc.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

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
                    "exists", false, // 회원 없음 => 회원가입 필요
                    "email", email,
                    "name", name
            );
        }

        User user = foundUser.get();

        // TODO 시간 바꾸기
        String jwtAccessToken = tokenProvider.generateGoogleLoginToken(user, Duration.ofMinutes(5));
        String refreshToken = tokenProvider.generateGoogleLoginToken(user, Duration.ofMinutes(10));
        refreshTokenService.saveRefreshToken(refreshToken, user, Duration.ofMinutes(10));

        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 10); // 2주로 변경하기
        response.addCookie(cookie);

        return Map.of(
                "exists", true, // 회원 존재 & 로그인
                "accessToken", jwtAccessToken
        );
    }
}
