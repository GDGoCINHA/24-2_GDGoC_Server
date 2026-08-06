package inha.gdgoc.domain.user.exception;

import inha.gdgoc.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    // 400 BAD REQUEST
    INVALID_MAJOR(HttpStatus.BAD_REQUEST, "알 수 없는 학과입니다."),
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "전화번호 형식이 올바르지 않습니다."),
    INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 파일입니다."),

    // 404 NOT FOUND
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 유저를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
