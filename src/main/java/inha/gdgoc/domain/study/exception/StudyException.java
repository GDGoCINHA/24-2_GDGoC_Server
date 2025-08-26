package inha.gdgoc.domain.study.exception;

import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.ErrorCode;

public class StudyException extends BusinessException {

    public StudyException(ErrorCode errorCode) {
        super(errorCode);
    }
}
