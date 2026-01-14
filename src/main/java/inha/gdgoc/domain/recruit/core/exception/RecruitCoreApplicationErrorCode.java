package inha.gdgoc.domain.recruit.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum RecruitCoreApplicationErrorCode {
    ALREADY_APPLIED("ALREADY_APPLIED", "이미 지원이 완료되었습니다.", HttpStatus.CONFLICT),
    APPLICATION_NOT_FOUND("APPLICATION_NOT_FOUND", "제출된 운영진 지원서가 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;

    RecruitCoreApplicationErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
