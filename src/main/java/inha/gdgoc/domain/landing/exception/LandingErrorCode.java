package inha.gdgoc.domain.landing.exception;

import inha.gdgoc.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum LandingErrorCode implements ErrorCode {

    LANDING_DRAFT_NOT_FOUND(HttpStatus.NOT_FOUND, "발행할 초안이 없습니다. 먼저 저장해 주세요."),
    LANDING_CONTENT_NOT_WRITABLE(HttpStatus.INTERNAL_SERVER_ERROR, "콘텐츠를 저장하지 못했습니다.");

    private final HttpStatus status;
    private final String message;

    LandingErrorCode(HttpStatus status, String message) {
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
