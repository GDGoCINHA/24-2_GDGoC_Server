package inha.gdgoc.domain.auth.controller;

import inha.gdgoc.domain.auth.dto.request.CodeVerificationRequest;
import inha.gdgoc.domain.auth.dto.request.PasswordResetRequest;
import inha.gdgoc.domain.auth.dto.request.SendingCodeRequest;
import inha.gdgoc.domain.auth.dto.request.UserLoginRequest;
import inha.gdgoc.domain.auth.dto.response.AccessTokenResponse;
import inha.gdgoc.domain.auth.dto.response.CodeVerificationResponse;
import inha.gdgoc.domain.auth.dto.response.LoginResponse;
import inha.gdgoc.domain.auth.exception.AuthErrorCode;
import inha.gdgoc.domain.auth.exception.AuthException;
import inha.gdgoc.domain.auth.service.AuthCodeService;
import inha.gdgoc.domain.auth.service.AuthService;
import inha.gdgoc.domain.auth.service.MailService;
import inha.gdgoc.domain.auth.service.RefreshTokenService;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.config.jwt.TokenProvider;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.exception.GlobalErrorCode;
import inha.gdgoc.global.security.AccessGuard;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;

import static inha.gdgoc.domain.auth.controller.message.AuthMessage.*;
import static inha.gdgoc.domain.auth.exception.AuthErrorCode.UNAUTHORIZED_USER;
import static inha.gdgoc.domain.auth.exception.AuthErrorCode.USER_NOT_FOUND;

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
    private final AccessGuard accessGuard;

    @GetMapping("/oauth2/google/callback")
    public ResponseEntity<ApiResponse<Map<String, Object>, Void>> handleGoogleCallback(@RequestParam String code, HttpServletResponse response) {
        Map<String, Object> data = authService.processOAuthLogin(code, response);

        return ResponseEntity.ok(ApiResponse.ok(OAUTH_LOGIN_SIGNUP_SUCCESS, data));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@CookieValue(value = "refresh_token", required = false) String refreshToken) {
        log.info("리프레시 토큰 요청 받음. 토큰 존재 여부: {}", refreshToken != null);

        if (refreshToken == null) {
            throw new AuthException(AuthErrorCode.INVALID_COOKIE);
        }

        try {
            String newAccessToken = refreshTokenService.refreshAccessToken(refreshToken);
            AccessTokenResponse accessTokenResponse = new AccessTokenResponse(newAccessToken);

            return ResponseEntity.ok(ApiResponse.ok(ACCESS_TOKEN_REFRESH_SUCCESS, accessTokenResponse, null));
        } catch (Exception e) {
            log.error("리프레시 토큰 처리 중 오류: {}", e.getMessage(), e);
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse, Void>> login(@Valid @RequestBody UserLoginRequest req, HttpServletResponse response) throws NoSuchAlgorithmException, InvalidKeyException {
        String email = req.email().trim();
        LoginResponse loginResponse = authService.loginWithPassword(email, req.password(), response);
        return ResponseEntity.ok(ApiResponse.ok(LOGIN_WITH_PASSWORD_SUCCESS, loginResponse));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void, Void>> logout() {
        // TODO 서비스로 넘기기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 1) 익명 방어
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new AuthException(UNAUTHORIZED_USER);
        }

        // 2) principal 캐스팅해서 확정적으로 userId/email 사용
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof TokenProvider.CustomUserDetails userDetails)) {
            throw new AuthException(UNAUTHORIZED_USER);
        }

        Long userId = userDetails.getUserId();
        String email = userDetails.getUsername();

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
    public ResponseEntity<ApiResponse<Void, Void>> responseResponseEntity(@RequestBody SendingCodeRequest sendingCodeRequest) {
        // TODO 서비스로 넘기기
        if (userRepository.existsByNameAndEmail(sendingCodeRequest.name(), sendingCodeRequest.email())) {
            String code = mailService.sendAuthCode(sendingCodeRequest.email());
            authCodeService.saveAuthCode(sendingCodeRequest.email(), code);

            return ResponseEntity.ok(ApiResponse.ok(CODE_CREATION_SUCCESS));
        }
        throw new AuthException(USER_NOT_FOUND);
    }

    @PostMapping("/password-reset/verify")
    public ResponseEntity<ApiResponse<CodeVerificationResponse, Void>> verifyCode(@RequestBody CodeVerificationRequest request) {
        // TODO 서비스 단 DTO 추가
        boolean verified = authCodeService.verify(request.email(), request.code());
        CodeVerificationResponse response = new CodeVerificationResponse(verified);

        return ResponseEntity.ok(ApiResponse.ok(PASSWORD_RESET_VERIFICATION_SUCCESS, response));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void, Void>> resetPassword(@RequestBody PasswordResetRequest passwordResetRequest) throws NoSuchAlgorithmException, InvalidKeyException {
        // TODO 서비스 단으로
        Optional<User> user = userRepository.findByEmail(passwordResetRequest.email());
        if (user.isEmpty()) {
            throw new AuthException(USER_NOT_FOUND);
        }

        User foundUser = user.get();
        foundUser.updatePassword(passwordResetRequest.password());
        userRepository.save(foundUser);

        return ResponseEntity.ok(ApiResponse.ok(PASSWORD_CHANGE_SUCCESS));
    }

    /**
     * 요구 권한(role) 이상이면 200, 아니면 403
     * 미인증이면 401

     * 예) /api/v1/auth/LEAD, /api/v1/auth/ORGANIZER, /api/v1/auth/ADMIN
     */
    @GetMapping("/{role}")
    public ResponseEntity<ApiResponse<Void, ?>> checkRoleOrTeam(
            @AuthenticationPrincipal TokenProvider.CustomUserDetails me,
            @PathVariable UserRole role,
            @RequestParam(value = "team", required = false) TeamType requiredTeam
    ) {
        if (me == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(
                            GlobalErrorCode.UNAUTHORIZED_USER.getStatus().value(),
                            GlobalErrorCode.UNAUTHORIZED_USER.getMessage(),
                            null
                    ));
        }

        var conditions = new java.util.ArrayList<AccessGuard.AccessCondition>();
        conditions.add(AccessGuard.AccessCondition.atLeast(role));

        if (requiredTeam != null) {
            conditions.add(AccessGuard.AccessCondition.atLeast(UserRole.ORGANIZER));
            conditions.add(AccessGuard.AccessCondition.of(UserRole.GUEST, requiredTeam));
        }

        if (accessGuard.check(me, conditions.toArray(AccessGuard.AccessCondition[]::new))) {
            return ResponseEntity.ok(ApiResponse.ok("ROLE_OR_TEAM_CHECK_PASSED", null));
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(
                        GlobalErrorCode.FORBIDDEN_USER.getStatus().value(),
                        GlobalErrorCode.FORBIDDEN_USER.getMessage(),
                        null
                ));
    }
}
