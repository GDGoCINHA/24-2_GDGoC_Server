package inha.gdgoc.domain.auth.exception;

import inha.gdgoc.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {

    // 403 Forbidden
    INVALID_COOKIE(HttpStatus.FORBIDDEN, "Refresh Token 이 비어있습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.FORBIDDEN, "잘못된 Refresh Token 값입니다.");

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
