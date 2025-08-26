package inha.gdgoc.global.exception;

import org.springframework.http.HttpStatus;

public enum GlobalErrorCode implements ErrorCode {

    // 400 Bad Request
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "요청 경로의 파라미터는 올바른 형식이 아닙니다."),
    INVALID_JSON_REQUEST(HttpStatus.BAD_REQUEST, "JSON 형식이 올바르지 않습니다."),
    MISSING_HEADER(HttpStatus.BAD_REQUEST, "요청 헤더 '%s'가 누락되었습니다."),

    // 403 FORBIDDEN
    INVALID_JWT_REQUEST(HttpStatus.FORBIDDEN, "잘못된 JWT 토큰입니다."),

    // 404 Not Found
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 리소스입니다."),

    // 405 Method Not Allowed
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 메서드입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다. 관리자에게 문의해 주세요.");

    private final HttpStatus status;
    private final String message;

    GlobalErrorCode(HttpStatus status, String message) {
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

    public String format(Object... args) {
        return String.format(this.message, args);
    }
}
