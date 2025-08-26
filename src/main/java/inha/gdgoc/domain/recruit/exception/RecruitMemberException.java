package inha.gdgoc.domain.recruit.exception;

import inha.gdgoc.global.error.BusinessException;
import inha.gdgoc.global.error.ErrorCode;

public class RecruitMemberException extends BusinessException {

    public RecruitMemberException(ErrorCode errorCode) {
        super(errorCode);
    }
}
