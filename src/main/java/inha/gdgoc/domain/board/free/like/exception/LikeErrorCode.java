package inha.gdgoc.domain.board.free.like.exception;

import inha.gdgoc.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum LikeErrorCode implements ErrorCode {

    ALREADY_LIKED(HttpStatus.CONFLICT, "이미 좋아요한 게시글입니다."),
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "좋아요하지 않은 게시글입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    LikeErrorCode(HttpStatus status, String message) {
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
