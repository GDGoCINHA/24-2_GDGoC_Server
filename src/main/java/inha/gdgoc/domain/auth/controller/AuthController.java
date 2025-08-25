package inha.gdgoc.domain.auth.controller;

import static inha.gdgoc.domain.auth.controller.message.AuthMessage.ACCESS_TOKEN_REFRESH_SUCCESS;
import static inha.gdgoc.domain.auth.controller.message.AuthMessage.CODE_CREATION_SUCCESS;
import static inha.gdgoc.domain.auth.controller.message.AuthMessage.LOGIN_WITH_PASSWORD_SUCCESS;
import static inha.gdgoc.domain.auth.controller.message.AuthMessage.LOGOUT_SUCCESS;
import static inha.gdgoc.domain.auth.controller.message.AuthMessage.OAUTH_LOGIN_SIGNUP_SUCCESS;
import static inha.gdgoc.domain.auth.controller.message.AuthMessage.PASSWORD_CHANGE_SUCCESS;
import static inha.gdgoc.domain.auth.controller.message.AuthMessage.PASSWORD_RESET_VERIFICATION_SUCCESS;
import static inha.gdgoc.domain.auth.exception.AuthErrorCode.UNAUTHORIZED_USER;
import static inha.gdgoc.domain.auth.exception.AuthErrorCode.USER_NOT_FOUND;

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
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.error.BusinessException;
import jakarta.servlet.http.HttpServletResponse;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/api/v1/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final MailService mailService;
    private final AuthCodeService authCodeService;

    @GetMapping("/oauth2/google/callback")
    public ResponseEntity<ApiResponse<Map<String, Object>, Void>> handleGoogleCallback(
            @RequestParam String code,
            HttpServletResponse response
    ) {
        Map<String, Object> data = authService.processOAuthLogin(code, response);
        return ResponseEntity.ok(ApiResponse.ok(OAUTH_LOGIN_SIGNUP_SUCCESS, data));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(
            @CookieValue(value = "refresh_token", required = false) String refreshToken
    ) {
        log.info("리프레시 토큰 요청 받음. 토큰 존재 여부: {}", refreshToken != null);

        if (refreshToken == null) {
            throw new BusinessException(AuthErrorCode.INVALID_COOKIE);
        }

        log.info("리프레시 토큰 값: {}", refreshToken);

        try {
            String newAccessToken = refreshTokenService.refreshAccessToken(refreshToken);
            AccessTokenResponse accessTokenResponse = new AccessTokenResponse(newAccessToken);

            return ResponseEntity.ok(
                    ApiResponse.ok(ACCESS_TOKEN_REFRESH_SUCCESS, accessTokenResponse, null));
        } catch (Exception e) {
            log.error("리프레시 토큰 처리 중 오류: {}", e.getMessage(), e);
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse, Void>> login(
            @RequestBody UserLoginRequest userLoginRequest,
            HttpServletResponse response
    ) throws NoSuchAlgorithmException, InvalidKeyException {
        LoginResponse loginResponse = authService.loginWithPassword(userLoginRequest, response);

        return ResponseEntity.ok(ApiResponse.ok(LOGIN_WITH_PASSWORD_SUCCESS, loginResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void, Void>> logout() {
        // TODO 서비스로 넘기기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(UNAUTHORIZED_USER);
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));
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
            log.warn("사용자를 찾을 수 없습니다.");
        }

        return ResponseEntity.ok(ApiResponse.ok(LOGOUT_SUCCESS));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<Void, Void>> responseResponseEntity(
            @RequestBody SendingCodeRequest sendingCodeRequest
    ) {
        // TODO 서비스로 넘기기
        if (userRepository.existsByNameAndEmail(sendingCodeRequest.name(),
                sendingCodeRequest.email())) {
            String code = mailService.sendAuthCode(sendingCodeRequest.email());
            authCodeService.saveAuthCode(sendingCodeRequest.email(), code);

            return ResponseEntity.ok(ApiResponse.ok(CODE_CREATION_SUCCESS));
        }
        throw new BusinessException(USER_NOT_FOUND);
    }

    @PostMapping("/password-reset/verify")
    public ResponseEntity<ApiResponse<CodeVerificationResponse, Void>> verifyCode(
            @RequestBody CodeVerificationRequest request
    ) {
        // TODO 서비스 단 DTO 추가
        boolean verified = authCodeService.verify(request.email(), request.code());
        CodeVerificationResponse response = new CodeVerificationResponse(verified);

        return ResponseEntity.ok(ApiResponse.ok(PASSWORD_RESET_VERIFICATION_SUCCESS, response));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void, Void>> resetPassword(
            @RequestBody PasswordResetRequest passwordResetRequest
    ) throws NoSuchAlgorithmException, InvalidKeyException {
        // TODO 서비스 단으로
        Optional<User> user = userRepository.findByEmail(passwordResetRequest.email());
        if (user.isEmpty()) {
            throw new BusinessException(USER_NOT_FOUND);
        }

        User foundUser = user.get();
        foundUser.updatePassword(passwordResetRequest.password());
        userRepository.save(foundUser);

        return ResponseEntity.ok(ApiResponse.ok(PASSWORD_CHANGE_SUCCESS));
    }
}
