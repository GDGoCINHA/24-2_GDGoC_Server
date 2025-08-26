package inha.gdgoc.domain.study.exception;

import inha.gdgoc.global.error.BusinessException;
import inha.gdgoc.global.error.ErrorCode;

public class StudyAttendeeException extends BusinessException {

    public StudyAttendeeException(ErrorCode errorCode) {
        super(errorCode);
    }
}
