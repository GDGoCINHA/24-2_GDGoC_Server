package inha.gdgoc.domain.recruit.member.exception;

import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.ErrorCode;

public class RecruitMemberException extends BusinessException {

    public RecruitMemberException(ErrorCode errorCode) {
        super(errorCode);
    }
}
