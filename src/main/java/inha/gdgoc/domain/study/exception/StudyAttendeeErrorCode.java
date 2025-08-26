package inha.gdgoc.domain.study.exception;

import inha.gdgoc.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum StudyAttendeeErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_PAGE(HttpStatus.BAD_REQUEST, "page가 1보다 작을 수 없습니다."),

    // 404 Not Found
    STUDY_ATTENDEE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 스터디에 지원한 지원자 정보가 없습니다."),

    // 409 Conflict
    STUDY_ALREADY_APPLIED(HttpStatus.CONFLICT, "이미 가입한 스터디입니다.");

    private final HttpStatus status;
    private final String message;

    StudyAttendeeErrorCode(HttpStatus status, String message) {
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
