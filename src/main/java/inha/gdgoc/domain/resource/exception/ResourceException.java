package inha.gdgoc.domain.resource.exception;

import inha.gdgoc.global.error.BusinessException;
import inha.gdgoc.global.error.ErrorCode;

public class ResourceException extends BusinessException {

    public ResourceException(ErrorCode errorCode) {
        super(errorCode);
    }
}
