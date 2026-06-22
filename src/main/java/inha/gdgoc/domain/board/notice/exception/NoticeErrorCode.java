package inha.gdgoc.domain.board.notice.exception;

import inha.gdgoc.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum NoticeErrorCode implements ErrorCode {

    NOTICE_PIN_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "고정 게시글은 최대 3개까지 가능합니다.");

    private final HttpStatus status;
    private final String message;

    NoticeErrorCode(HttpStatus status, String message) {
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
