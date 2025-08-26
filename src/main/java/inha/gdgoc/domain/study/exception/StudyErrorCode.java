package inha.gdgoc.domain.study.exception;

import inha.gdgoc.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum StudyErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_PAGE(HttpStatus.BAD_REQUEST, "page가 1보다 작을 수 없습니다."),

    // 403 FORBIDDEN
    STUDY_APPLICANT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인이 만든 스터디의 지원자 정보만 확인할 수 있습니다."),

    // 404 NOT FOUND
    STUDY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 스터디입니다.");

    private final HttpStatus status;
    private final String message;

    StudyErrorCode(HttpStatus status, String message) {
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
