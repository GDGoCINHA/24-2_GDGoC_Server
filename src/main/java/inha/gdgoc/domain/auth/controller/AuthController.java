package inha.gdgoc.domain.auth.controller;

import static inha.gdgoc.domain.auth.controller.message.AuthMessage.*;

import inha.gdgoc.domain.auth.dto.request.CheckPhoneNumberRequest;
import inha.gdgoc.domain.auth.dto.request.CheckStudentIdRequest;
import inha.gdgoc.domain.auth.dto.request.LoginRequest;
import inha.gdgoc.domain.auth.dto.request.SignupRequest;
import inha.gdgoc.domain.auth.dto.request.TokenRefreshRequest;
import inha.gdgoc.domain.auth.dto.response.AccessTokenResponse;
import inha.gdgoc.domain.auth.dto.response.AuthUserResponse;
import inha.gdgoc.domain.auth.dto.response.CheckPhoneNumberResponse;
import inha.gdgoc.domain.auth.dto.response.CheckStudentIdResponse;
import inha.gdgoc.domain.auth.exception.AuthErrorCode;
import inha.gdgoc.domain.auth.exception.AuthException;
import inha.gdgoc.domain.auth.service.AuthService;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.exception.GlobalErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping("/api/v1/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 1. 구글 로그인 (ID Token 검증)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Object response = authService.login(request.getIdToken());
            return ResponseEntity.ok().body(ApiResponse.ok(LOGIN_SUCCESS, response));
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
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(SIGNUP_SUCCESS, response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    @PostMapping("/check/student-id")
    public ResponseEntity<ApiResponse<CheckStudentIdResponse, Void>> duplicatedStudentIdDetails(
            @Valid @RequestBody CheckStudentIdRequest request
    ) {
        CheckStudentIdResponse response = authService.isRegisteredStudentId(request.getStudentId());
        return ResponseEntity.ok(ApiResponse.ok(STUDENT_ID_DUPLICATION_CHECK_SUCCESS, response));
    }

    @PostMapping("/check/phone-number")
    public ResponseEntity<ApiResponse<CheckPhoneNumberResponse, Void>> duplicatedPhoneNumberDetails(
            @Valid @RequestBody CheckPhoneNumberRequest request
    ) {
        CheckPhoneNumberResponse response = authService.isRegisteredPhoneNumber(request.getPhoneNumber());
        return ResponseEntity.ok(ApiResponse.ok(PHONE_NUMBER_DUPLICATION_CHECK_SUCCESS, response));
    }

    // 3. 토큰 재발급 (Refresh)
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@Valid @RequestBody TokenRefreshRequest request) {
        try {
            AuthService.RefreshResult result = authService.refresh(request.getRefreshToken());
            return ResponseEntity.ok()
                    .body(ApiResponse.ok(
                            ACCESS_TOKEN_REFRESH_SUCCESS,
                            new AccessTokenResponse(result.accessToken(), AuthUserResponse.from(result.user()))
                    ));
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    // 4. 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) TokenRefreshRequest request) {
        if (request == null || !StringUtils.hasText(request.getRefreshToken())) {
            log.warn("로그아웃 실패: 요청 바디에 리프레시 토큰이 누락되었습니다.");
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "리프레시 토큰은 필수입니다.", null));
        }
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok().body(ApiResponse.ok(LOGOUT_SUCCESS));
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
        if (authService.hasRequiredAccess(me, role, requiredTeam)) {
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
