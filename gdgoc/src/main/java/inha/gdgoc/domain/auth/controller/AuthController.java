package inha.gdgoc.domain.auth.controller;

import inha.gdgoc.domain.auth.dto.request.FindIdRequest;
import inha.gdgoc.domain.auth.dto.request.UserSignupRequest;
import inha.gdgoc.domain.auth.dto.response.AccessTokenResponse;
import inha.gdgoc.domain.auth.dto.response.FindIdResponse;
import inha.gdgoc.domain.auth.service.AuthService;
import inha.gdgoc.domain.auth.service.GoogleOAuthService;
import inha.gdgoc.domain.auth.service.RefreshTokenService;
import inha.gdgoc.global.common.ApiResponse;
import inha.gdgoc.global.common.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping("/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final GoogleOAuthService googleOAuthService;
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @GetMapping("/oauth2/google/callback")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleGoogleCallback(
            @RequestParam String code,
            HttpServletResponse response
    ) {
        Map<String, Object> data = googleOAuthService.processOAuthLogin(code, response);
        return ResponseEntity.ok(ApiResponse.of(data, null));
    }

    // TODO 학번 중복 조회

    // TODO 이메일 중복 조회

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> userSignup(
            @RequestBody UserSignupRequest userSignupRequest) {
        authService.saveUser(userSignupRequest);
        return ResponseEntity.ok(ApiResponse.of(null, null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(
            @CookieValue(value = "refresh_token", required = false) String refreshToken) {

        log.info("리프레시 토큰 요청 받음. 토큰 존재 여부: {}", refreshToken != null);

        if (refreshToken == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Refresh token is missing."));
        }

        log.info("리프레시 토큰 값: {}", refreshToken);

        try {
            String newAccessToken = refreshTokenService.refreshAccessToken(refreshToken);
            AccessTokenResponse accessTokenResponse = new AccessTokenResponse(newAccessToken);
            return ResponseEntity.ok(ApiResponse.of(accessTokenResponse, null));
        } catch (Exception e) {
            log.error("리프레시 토큰 처리 중 오류: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid refresh token: " + e.getMessage()));
        }
    }

    // TODO 자체 로그인

    // TODO 로그아웃

    @GetMapping("/findId")
    public ResponseEntity<?> findId(@RequestBody FindIdRequest findIdRequest) {
        try {
            FindIdResponse response = authService.findId(findIdRequest);
            return ResponseEntity.ok(ApiResponse.of(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("해당 정보로 가입된 사용자가 없습니다."));
        }
    }
}
