package inha.gdgoc.domain.auth.exception;

import inha.gdgoc.global.error.BusinessException;
import inha.gdgoc.global.error.ErrorCode;

public class AuthException extends BusinessException {

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }
}
