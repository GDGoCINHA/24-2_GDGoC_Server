package inha.gdgoc.domain.auth.controller;

import inha.gdgoc.domain.auth.dto.request.LoginRequest;
import inha.gdgoc.domain.auth.dto.request.SignupRequest;
import inha.gdgoc.domain.auth.dto.response.AccessTokenResponse;
import inha.gdgoc.domain.auth.dto.response.AuthUserResponse;
import inha.gdgoc.domain.auth.dto.response.CheckPhoneNumberResponse;
import inha.gdgoc.domain.auth.dto.response.CheckStudentIdResponse;
import inha.gdgoc.domain.auth.dto.response.LoginSuccessResponse;
import inha.gdgoc.domain.auth.exception.AuthErrorCode;
import inha.gdgoc.domain.auth.exception.AuthException;
import inha.gdgoc.domain.auth.service.AuthService;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.JwtProperties;
import inha.gdgoc.global.config.jwt.TokenProvider;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.exception.GlobalErrorCode;
import inha.gdgoc.global.security.AccessGuard;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.time.Duration;

import static inha.gdgoc.domain.auth.controller.message.AuthMessage.*;

@Slf4j
@RequestMapping("/api/v1/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AccessGuard accessGuard;
    private final JwtProperties jwtProperties;
    @Value("${app.auth.refresh-cookie.domain:}")
    private String refreshCookieDomain;

    @Value("${app.auth.refresh-cookie.path:/}")
    private String refreshCookiePath;

    @Value("${app.auth.refresh-cookie.same-site:Lax}")
    private String refreshCookieSameSite;

    @Value("${app.auth.refresh-cookie.secure:false}")
    private boolean refreshCookieSecure;

    @Value("${app.auth.access-cookie.domain:}")
    private String accessCookieDomain;

    @Value("${app.auth.access-cookie.path:/}")
    private String accessCookiePath;

    @Value("${app.auth.access-cookie.same-site:Lax}")
    private String accessCookieSameSite;

    @Value("${app.auth.access-cookie.secure:false}")
    private boolean accessCookieSecure;

    // 1. 구글 로그인 (ID Token 검증)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Object response = authService.login(request.getIdToken());
            ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
            if (response instanceof LoginSuccessResponse successResponse) {
                ResponseCookie cookie = buildRefreshTokenCookie(successResponse.getRefreshToken());
                builder.header(HttpHeaders.SET_COOKIE, cookie.toString());
                ResponseCookie accessCookie = buildAccessTokenCookie(successResponse.getAccessToken());
                builder.header(HttpHeaders.SET_COOKIE, accessCookie.toString());
            }
            return builder.body(ApiResponse.ok(LOGIN_SUCCESS, response));
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
            ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.CREATED);
            if (response instanceof LoginSuccessResponse successResponse) {
                ResponseCookie cookie = buildRefreshTokenCookie(successResponse.getRefreshToken());
                builder.header(HttpHeaders.SET_COOKIE, cookie.toString());
                ResponseCookie accessCookie = buildAccessTokenCookie(successResponse.getAccessToken());
                builder.header(HttpHeaders.SET_COOKIE, accessCookie.toString());
            }
            return builder.body(ApiResponse.ok(SIGNUP_SUCCESS, response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    @GetMapping("/check/student-id")
    public ResponseEntity<ApiResponse<CheckStudentIdResponse, Void>> duplicatedStudentIdDetails(
            @RequestParam
            @NotBlank(message = "학번은 필수 입력 값입니다.")
            @Pattern(regexp = "^12[0-9]{6}$", message = "유효하지 않은 학번 값입니다.")
            String studentId
    ) {
        CheckStudentIdResponse response = authService.isRegisteredStudentId(studentId);
        return ResponseEntity.ok(ApiResponse.ok(STUDENT_ID_DUPLICATION_CHECK_SUCCESS, response));
    }

    @GetMapping("/check/phone-number")
    public ResponseEntity<ApiResponse<CheckPhoneNumberResponse, Void>> duplicatedPhoneNumberDetails(
            @RequestParam
            @NotBlank(message = "전화번호는 필수 입력 값입니다.")
            @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호 형식은 010-XXXX-XXXX 이어야 합니다.")
            String phoneNumber
    ) {
        CheckPhoneNumberResponse response = authService.isRegisteredPhoneNumber(phoneNumber);
        return ResponseEntity.ok(ApiResponse.ok(PHONE_NUMBER_DUPLICATION_CHECK_SUCCESS, response));
    }

    // 3. 토큰 재발급 (Refresh)
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@CookieValue(value = "refresh_token", required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new AuthException(AuthErrorCode.INVALID_COOKIE);
        }

        try {
            AuthService.RefreshResult result = authService.refresh(refreshToken);
            ResponseCookie accessCookie = buildAccessTokenCookie(result.accessToken());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
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
    public ResponseEntity<?> logout(@CookieValue(value = "refresh_token", required = false) String refreshToken) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        builder.header(HttpHeaders.SET_COOKIE, deleteRefreshTokenCookie().toString());
        builder.header(HttpHeaders.SET_COOKIE, deleteAccessTokenCookie().toString());
        return builder.body(ApiResponse.ok(LOGOUT_SUCCESS));
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

    private ResponseCookie buildRefreshTokenCookie(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return deleteRefreshTokenCookie();
        }
        return baseCookieBuilder(refreshToken)
                .maxAge(AuthService.REFRESH_TOKEN_TTL)
                .build();
    }

    private ResponseCookie deleteRefreshTokenCookie() {
        return baseCookieBuilder("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookieBuilder(String value) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("refresh_token", value)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path(refreshCookiePath);

        if (StringUtils.hasText(refreshCookieDomain)) {
            builder.domain(refreshCookieDomain);
        }
        return builder;
    }

    private ResponseCookie buildAccessTokenCookie(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return deleteAccessTokenCookie();
        }
        ResponseCookie.ResponseCookieBuilder builder = baseAccessCookieBuilder(accessToken);
        long accessTokenValidity = jwtProperties.getAccessTokenValidity();
        if (accessTokenValidity > 0) {
            builder.maxAge(Duration.ofMillis(accessTokenValidity));
        }
        return builder.build();
    }

    private ResponseCookie deleteAccessTokenCookie() {
        return baseAccessCookieBuilder("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseAccessCookieBuilder(String value) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("access_token", value)
                .httpOnly(true)
                .secure(accessCookieSecure)
                .sameSite(accessCookieSameSite)
                .path(accessCookiePath);

        if (StringUtils.hasText(accessCookieDomain)) {
            builder.domain(accessCookieDomain);
        }
        return builder;
    }
}
