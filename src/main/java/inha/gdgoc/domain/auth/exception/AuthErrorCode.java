package inha.gdgoc.domain.auth.exception;

import inha.gdgoc.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {

    // 401 Unauthorized
    UNAUTHORIZED_USER(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다."),

    // 403 Forbidden
    INVALID_COOKIE(HttpStatus.FORBIDDEN, "Refresh Token 이 비어있습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.FORBIDDEN, "잘못된 Refresh Token 값입니다."),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다");

    private final HttpStatus status;
    private final String message;

    AuthErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
