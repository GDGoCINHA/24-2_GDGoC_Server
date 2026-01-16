package inha.gdgoc.domain.auth.controller;

import inha.gdgoc.domain.auth.dto.request.LoginRequest;
import inha.gdgoc.domain.auth.dto.request.SignupRequest;
import inha.gdgoc.domain.auth.dto.response.AccessTokenResponse;
import inha.gdgoc.domain.auth.exception.AuthErrorCode;
import inha.gdgoc.domain.auth.exception.AuthException;
import inha.gdgoc.domain.auth.service.AuthService;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.exception.GlobalErrorCode;
import inha.gdgoc.global.security.AccessGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static inha.gdgoc.domain.auth.controller.message.AuthMessage.*;

@Slf4j
@RequestMapping("/api/v1/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AccessGuard accessGuard;

    // 1. 구글 로그인 (ID Token 검증)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Object response = authService.login(request.getIdToken());
            return ResponseEntity.ok(ApiResponse.ok(LOGIN_SUCCESS, response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(AuthErrorCode.INVALID_TOKEN.getStatus().value(), e.getMessage(), null));
        }
    }

    // 2. 회원가입 (추가 정보 입력)
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        try {
            Object response = authService.signup(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(SIGNUP_SUCCESS, response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    // 3. 토큰 재발급 (Refresh)
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

    // 4. 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(value = "refresh_token", required = false) String refreshToken) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.ok(ApiResponse.ok(LOGOUT_SUCCESS));
    }

    // 5. 권한 체크 (Role or Team)
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