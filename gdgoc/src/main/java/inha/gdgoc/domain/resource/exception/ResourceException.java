package inha.gdgoc.domain.resource.exception;

import inha.gdgoc.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ResourceException implements ErrorCode {

    // 413
    INVALID_BIG_FILE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기는 10Mb를 넘을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    ResourceException(HttpStatus status, String message) {
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
