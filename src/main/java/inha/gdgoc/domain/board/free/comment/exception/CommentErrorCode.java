package inha.gdgoc.domain.board.free.comment.exception;

import inha.gdgoc.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CommentErrorCode implements ErrorCode {

    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),
    NOT_COMMENT_OWNER(HttpStatus.FORBIDDEN, "본인의 댓글만 수정/삭제할 수 있습니다."),
    AUTHOR_NOT_FOUND(HttpStatus.NOT_FOUND, "작성자 정보를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    CommentErrorCode(HttpStatus status, String message) {
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
