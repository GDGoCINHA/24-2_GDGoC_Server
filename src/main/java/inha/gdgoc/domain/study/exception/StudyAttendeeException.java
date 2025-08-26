package inha.gdgoc.domain.study.exception;

import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.ErrorCode;

public class StudyAttendeeException extends BusinessException {

    public StudyAttendeeException(ErrorCode errorCode) {
        super(errorCode);
    }
}
