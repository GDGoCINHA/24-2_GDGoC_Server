package inha.gdgoc.domain.resource.exception;

import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.ErrorCode;

public class ResourceException extends BusinessException {

    public ResourceException(ErrorCode errorCode) {
        super(errorCode);
    }
}
