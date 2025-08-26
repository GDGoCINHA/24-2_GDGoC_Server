package inha.gdgoc.domain.auth.exception;

import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.ErrorCode;

public class AuthException extends BusinessException {

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }
}
