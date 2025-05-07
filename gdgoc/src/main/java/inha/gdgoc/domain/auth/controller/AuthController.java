package inha.gdgoc.domain.auth.controller;

import inha.gdgoc.domain.auth.dto.request.CodeVerificationRequest;
import inha.gdgoc.domain.auth.dto.request.PasswordResetRequest;
import inha.gdgoc.domain.auth.dto.request.SendingCodeRequest;
import inha.gdgoc.domain.auth.dto.request.UserLoginRequest;
import inha.gdgoc.domain.auth.dto.response.AccessTokenResponse;
import inha.gdgoc.domain.auth.dto.response.CodeVerificationResponse;
import inha.gdgoc.domain.auth.dto.response.LoginResponse;
import inha.gdgoc.domain.auth.exception.AuthErrorCode;
import inha.gdgoc.domain.auth.service.AuthCodeService;
import inha.gdgoc.domain.auth.service.AuthService;
import inha.gdgoc.domain.auth.service.MailService;
import inha.gdgoc.domain.auth.service.RefreshTokenService;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.common.ApiResponse;
import inha.gdgoc.global.common.ErrorResponse;

import inha.gdgoc.global.error.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private final UserRepository userRepository;
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final MailService mailService;
    private final AuthCodeService authCodeService;

    @GetMapping("/oauth2/google/callback")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleGoogleCallback(
            @RequestParam String code,
            HttpServletResponse response
    ) {
        Map<String, Object> data = authService.processOAuthLogin(code, response);
        return ResponseEntity.ok(ApiResponse.of(data, null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(
            @CookieValue(value = "refresh_token", required = false) String refreshToken) {

        log.info("리프레시 토큰 요청 받음. 토큰 존재 여부: {}", refreshToken != null);

        if (refreshToken == null) {
            throw new BusinessException(AuthErrorCode.INVALID_COOKIE);
        }

        log.info("리프레시 토큰 값: {}", refreshToken);

        try {
            String newAccessToken = refreshTokenService.refreshAccessToken(refreshToken);
            AccessTokenResponse accessTokenResponse = new AccessTokenResponse(newAccessToken);
            return ResponseEntity.ok(ApiResponse.of(accessTokenResponse, null));
        } catch (Exception e) {
            log.error("리프레시 토큰 처리 중 오류: {}", e.getMessage(), e);
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody UserLoginRequest userLoginRequest,
                                                            HttpServletResponse response)
            throws NoSuchAlgorithmException, InvalidKeyException {
        LoginResponse loginResponse = authService.loginWithPassword(userLoginRequest, response);
        return ResponseEntity.ok(ApiResponse.of(loginResponse, null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.of(null, "인증되지 않은 사용자"));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        Long userId = user.getId();

        log.info("로그아웃 시도: 사용자 ID: {}, 이메일: {}", userId, email);

        if (userId != null) {
            boolean deleted = refreshTokenService.logout(userId);

            if (!deleted) {
                log.warn("사용자 ID: {}의 리프레시 토큰 삭제에 실패했습니다.", userId);
            } else {
                log.info("사용자 ID: {}의 리프레시 토큰이 성공적으로 삭제되었습니다.", userId);
            }
        } else {
            log.warn("사용자 ID: {}의 쿠키에서 리프레시 토큰을 찾을 수 없습니다", userId);
        }

        return ResponseEntity.ok(ApiResponse.of(null, null));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<Void>> responseResponseEntity(
            @RequestBody SendingCodeRequest sendingCodeRequest
    ) {
        if (userRepository.existsByNameAndEmail(sendingCodeRequest.name(), sendingCodeRequest.email())) {
            String code = mailService.sendAuthCode(sendingCodeRequest.email());
            authCodeService.saveAuthCode(sendingCodeRequest.email(), code);
            return ResponseEntity.ok(ApiResponse.of(null));
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.of(null, null));
    }

    @PostMapping("/password-reset/verify")
    public ResponseEntity<ApiResponse<CodeVerificationResponse>> verifyCode(
            @RequestBody CodeVerificationRequest codeVerificationRequest
    ) {
        return ResponseEntity.ok(ApiResponse.of(new CodeVerificationResponse(authCodeService.verify(
                codeVerificationRequest.email(), codeVerificationRequest.code()))));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody PasswordResetRequest passwordResetRequest)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Optional<User> user = userRepository.findByEmail(passwordResetRequest.email());
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.of(null, null));
        }

        User foundUser = user.get();
        foundUser.updatePassword(passwordResetRequest.password());
        userRepository.save(foundUser);

        return ResponseEntity.ok(ApiResponse.of(null, null));
    }
}
