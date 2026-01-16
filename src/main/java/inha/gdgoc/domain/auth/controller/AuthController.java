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

    private final AuthService authService;

    /**
     * 구글 로그인 (ID Token 검증)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // AuthService에서 로그인 or 회원가입 필요 응답 분기 처리 결과 반환
            Object response = authService.login(request.getIdToken());
            return ResponseEntity.ok(ApiResponse.ok(LOGIN_SUCCESS, response)); // LOGIN_SUCCESS 메시지 필요 (없으면 기존 것 사용)
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(AuthErrorCode.INVALID_TOKEN.getStatus().value(), e.getMessage(), null));
        }
    }

    /**
     * 회원가입 (추가 정보 입력)
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        try {
            Object response = authService.signup(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(SIGNUP_SUCCESS, response)); // SIGNUP_SUCCESS 메시지 필요
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    /**
     * 토큰 재발급 (Refresh)
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@CookieValue(value = "refresh_token", required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new AuthException(AuthErrorCode.INVALID_COOKIE);
        }

        try {
            String newAccessToken = authService.refresh(refreshToken);
            return ResponseEntity.ok(ApiResponse.ok(ACCESS_TOKEN_REFRESH_SUCCESS, new AccessTokenResponse(newAccessToken)));
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(value = "refresh_token", required = false) String refreshToken) {
        // 리프레시 토큰이 없으면 그냥 성공 처리 (이미 로그아웃된 상태로 간주)
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.ok(ApiResponse.ok(LOGOUT_SUCCESS));
    }

    /**
     * 권한 체크 (Role or Team)
     */
    @GetMapping("/{role}")
    public ResponseEntity<ApiResponse<Void, ?>> checkRoleOrTeam(@AuthenticationPrincipal TokenProvider.CustomUserDetails me, 
                                                                @PathVariable UserRole role, 
                                                                @RequestParam(value = "team", required = false) TeamType requiredTeam) {
        if (me == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(GlobalErrorCode.UNAUTHORIZED_USER.getStatus().value(), 
                                          GlobalErrorCode.UNAUTHORIZED_USER.getMessage(), null));
        }

        // Role Check
        boolean roleOk = UserRole.hasAtLeast(me.getRole(), role);

        // Team Check
        boolean teamOk = false;
        if (requiredTeam != null) {
            if (UserRole.hasAtLeast(me.getRole(), UserRole.ORGANIZER)) {
                teamOk = true;
            } else {
                teamOk = (me.getTeam() != null && me.getTeam() == requiredTeam);
            }
        }

        if (roleOk || teamOk) {
            return ResponseEntity.ok(ApiResponse.ok("ROLE_OR_TEAM_CHECK_PASSED", null));
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(GlobalErrorCode.FORBIDDEN_USER.getStatus().value(), 
                                      GlobalErrorCode.FORBIDDEN_USER.getMessage(), null));
    }
}
// public class AuthController {

//     private final UserRepository userRepository;
//     private final AuthService authService;
//     private final RefreshTokenService refreshTokenService;
//     private final MailService mailService;
//     private final AuthCodeService authCodeService;

//     @GetMapping("/oauth2/google/callback")
//     public ResponseEntity<ApiResponse<Map<String, Object>, Void>> handleGoogleCallback(@RequestParam String code, HttpServletResponse response) {
//         Map<String, Object> data = authService.processOAuthLogin(code, response);

//         return ResponseEntity.ok(ApiResponse.ok(OAUTH_LOGIN_SIGNUP_SUCCESS, data));
//     }

//     @PostMapping("/refresh")
//     public ResponseEntity<?> refreshAccessToken(@CookieValue(value = "refresh_token", required = false) String refreshToken) {
//         log.info("리프레시 토큰 요청 받음. 토큰 존재 여부: {}", refreshToken != null);

//         if (refreshToken == null) {
//             throw new AuthException(AuthErrorCode.INVALID_COOKIE);
//         }

//         try {
//             String newAccessToken = refreshTokenService.refreshAccessToken(refreshToken);
//             AccessTokenResponse accessTokenResponse = new AccessTokenResponse(newAccessToken);

//             return ResponseEntity.ok(ApiResponse.ok(ACCESS_TOKEN_REFRESH_SUCCESS, accessTokenResponse, null));
//         } catch (Exception e) {
//             log.error("리프레시 토큰 처리 중 오류: {}", e.getMessage(), e);
//             throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
//         }
//     }

//     @PostMapping("/login")
//     public ResponseEntity<ApiResponse<LoginResponse, Void>> login(@Valid @RequestBody UserLoginRequest req, HttpServletResponse response) throws NoSuchAlgorithmException, InvalidKeyException {
//         String email = req.email().trim();
//         LoginResponse loginResponse = authService.loginWithPassword(email, req.password(), response);
//         return ResponseEntity.ok(ApiResponse.ok(LOGIN_WITH_PASSWORD_SUCCESS, loginResponse));
//     }

//     @PostMapping("/logout")
//     @PreAuthorize("isAuthenticated()")
//     public ResponseEntity<ApiResponse<Void, Void>> logout() {
//         // TODO 서비스로 넘기기
//         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

//         // 1) 익명 방어
//         if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
//             throw new AuthException(UNAUTHORIZED_USER);
//         }

//         // 2) principal 캐스팅해서 확정적으로 userId/email 사용
//         Object principal = authentication.getPrincipal();
//         if (!(principal instanceof TokenProvider.CustomUserDetails userDetails)) {
//             throw new AuthException(UNAUTHORIZED_USER);
//         }

//         Long userId = userDetails.getUserId();
//         String email = userDetails.getUsername();

//         log.info("로그아웃 시도: 사용자 ID: {}, 이메일: {}", userId, email);

//         if (userId != null) {
//             boolean deleted = refreshTokenService.logout(userId);

//             if (!deleted) {
//                 log.warn("사용자 ID: {}의 리프레시 토큰 삭제에 실패했습니다.", userId);
//             } else {
//                 log.info("사용자 ID: {}의 리프레시 토큰이 성공적으로 삭제되었습니다.", userId);
//             }
//         } else {
//             log.warn("사용자를 찾을 수 없습니다.");
//         }

//         return ResponseEntity.ok(ApiResponse.ok(LOGOUT_SUCCESS));
//     }

//     @PostMapping("/password-reset/request")
//     public ResponseEntity<ApiResponse<Void, Void>> responseResponseEntity(@RequestBody SendingCodeRequest sendingCodeRequest) {
//         // TODO 서비스로 넘기기
//         if (userRepository.existsByNameAndEmail(sendingCodeRequest.name(), sendingCodeRequest.email())) {
//             String code = mailService.sendAuthCode(sendingCodeRequest.email());
//             authCodeService.saveAuthCode(sendingCodeRequest.email(), code);

//             return ResponseEntity.ok(ApiResponse.ok(CODE_CREATION_SUCCESS));
//         }
//         throw new AuthException(USER_NOT_FOUND);
//     }

//     @PostMapping("/password-reset/verify")
//     public ResponseEntity<ApiResponse<CodeVerificationResponse, Void>> verifyCode(@RequestBody CodeVerificationRequest request) {
//         // TODO 서비스 단 DTO 추가
//         boolean verified = authCodeService.verify(request.email(), request.code());
//         CodeVerificationResponse response = new CodeVerificationResponse(verified);

//         return ResponseEntity.ok(ApiResponse.ok(PASSWORD_RESET_VERIFICATION_SUCCESS, response));
//     }

//     @PostMapping("/password-reset/confirm")
//     public ResponseEntity<ApiResponse<Void, Void>> resetPassword(@RequestBody PasswordResetRequest passwordResetRequest) throws NoSuchAlgorithmException, InvalidKeyException {
//         // TODO 서비스 단으로
//         Optional<User> user = userRepository.findByEmail(passwordResetRequest.email());
//         if (user.isEmpty()) {
//             throw new AuthException(USER_NOT_FOUND);
//         }

//         User foundUser = user.get();
//         foundUser.updatePassword(passwordResetRequest.password());
//         userRepository.save(foundUser);

//         return ResponseEntity.ok(ApiResponse.ok(PASSWORD_CHANGE_SUCCESS));
//     }

//     /**
//      * 요구 권한(role) 이상이면 200, 아니면 403
//      * 미인증이면 401

//      * 예) /api/v1/auth/LEAD, /api/v1/auth/ORGANIZER, /api/v1/auth/ADMIN
//      */
//     @GetMapping("/{role}")
//     public ResponseEntity<ApiResponse<Void, ?>> checkRoleOrTeam(@AuthenticationPrincipal TokenProvider.CustomUserDetails me, @PathVariable UserRole role, @RequestParam(value = "team", required = false) TeamType requiredTeam) {
//         // 1) 인증 체크
//         if (me == null) {
//             return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                     .body(ApiResponse.error(GlobalErrorCode.UNAUTHORIZED_USER.getStatus()
//                             .value(), GlobalErrorCode.UNAUTHORIZED_USER.getMessage(), null));
//         }

//         // 2) role check
//         final boolean roleOk = UserRole.hasAtLeast(me.getRole(), role);

//         // 3) team check if team parameter exists
//         boolean teamOk = false;
//         if (requiredTeam != null) {
//             if (UserRole.hasAtLeast(me.getRole(), UserRole.ORGANIZER)) {
//                 teamOk = true;
//             } else {
//                 teamOk = (me.getTeam() != null && me.getTeam() == requiredTeam);
//             }
//         }

//         // 4) OR 조건으로 최종 판정
//         if (roleOk || teamOk) {
//             return ResponseEntity.ok(ApiResponse.ok("ROLE_OR_TEAM_CHECK_PASSED", null));
//         }

//         return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                 .body(ApiResponse.error(GlobalErrorCode.FORBIDDEN_USER.getStatus()
//                         .value(), GlobalErrorCode.FORBIDDEN_USER.getMessage(), null));
//     }
// }
