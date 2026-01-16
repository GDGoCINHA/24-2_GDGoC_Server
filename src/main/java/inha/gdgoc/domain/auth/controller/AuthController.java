package inha.gdgoc.domain.auth.controller;



import inha.gdgoc.domain.auth.dto.request.CodeVerificationRequest;
import inha.gdgoc.domain.auth.dto.request.PasswordResetRequest;
import inha.gdgoc.domain.auth.dto.request.SendingCodeRequest;
import inha.gdgoc.domain.auth.dto.request.UserLoginRequest;
import inha.gdgoc.domain.auth.dto.response.AccessTokenResponse;
import inha.gdgoc.domain.auth.dto.response.CodeVerificationResponse;
import inha.gdgoc.domain.auth.dto.response.LoginResponse;
import inha.gdgoc.domain.auth.dto.request.LoginRequest;
import inha.gdgoc.domain.auth.dto.request.SignupRequest;
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

import inha.gdgoc.domain.auth.exception.AuthErrorCode;
import static inha.gdgoc.domain.auth.controller.message.AuthMessage.*;
import static inha.gdgoc.domain.auth.exception.AuthErrorCode.UNAUTHORIZED_USER;
import static inha.gdgoc.domain.auth.exception.AuthErrorCode.USER_NOT_FOUND;

@Slf4j
@RequestMapping("/api/v1/auth")
@RestController
@RequiredArgsConstructor

public class AuthController {

    private final AuthService authService;
    private final AccessGuard accessGuard;

    
    //구글 로그인 (ID Token 검증)
     
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

    
    // 회원가입 (추가 정보 입력)
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


    // 토큰 재발급 (Refresh)
    
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

    
    //  로그아웃
     
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(value = "refresh_token", required = false) String refreshToken) {
        // 리프레시 토큰이 없으면 그냥 성공 처리 (이미 로그아웃된 상태로 간주)
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.ok(ApiResponse.ok(LOGOUT_SUCCESS));
    }

    
     // 권한 체크 (Role or Team)
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
