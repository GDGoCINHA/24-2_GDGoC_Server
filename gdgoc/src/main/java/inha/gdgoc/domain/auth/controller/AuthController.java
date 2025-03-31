package inha.gdgoc.domain.auth.controller;

import inha.gdgoc.domain.auth.dto.request.UserSignupRequest;
import inha.gdgoc.domain.auth.dto.response.AccessTokenResponse;
import inha.gdgoc.domain.auth.service.AuthService;
import inha.gdgoc.domain.auth.service.GoogleOAuthService;
import inha.gdgoc.domain.auth.service.RefreshTokenService;
import inha.gdgoc.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> userSignup(
            @RequestBody UserSignupRequest userSignupRequest) {
        authService.saveUser(userSignupRequest);
        return ResponseEntity.ok(ApiResponse.success("회원가입 성공"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> refreshToken(
            @RequestHeader("Authorization") String refreshToken) {
        String newAccessToken = refreshTokenService.refreshAccessToken(refreshToken);
        AccessTokenResponse data = new AccessTokenResponse(newAccessToken);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

}
