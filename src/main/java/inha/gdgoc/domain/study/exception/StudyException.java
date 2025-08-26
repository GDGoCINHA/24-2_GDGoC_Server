package inha.gdgoc.domain.study.exception;

import inha.gdgoc.global.error.BusinessException;
import inha.gdgoc.global.error.ErrorCode;

public class StudyException extends BusinessException {

    public StudyException(ErrorCode errorCode) {
        super(errorCode);
    }
}
