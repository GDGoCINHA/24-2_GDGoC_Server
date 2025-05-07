package inha.gdgoc.global.common;

import inha.gdgoc.global.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class ErrorResponse {

    private final int status;
    private final String message;

    public ErrorResponse(ErrorCode errorCode) {
        this.status = errorCode.getStatus().value();
        this.message = errorCode.getMessage();
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
