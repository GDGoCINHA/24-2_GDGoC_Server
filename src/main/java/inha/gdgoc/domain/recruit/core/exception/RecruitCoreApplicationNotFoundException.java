package inha.gdgoc.domain.recruit.core.exception;

import lombok.Getter;

@Getter
public class RecruitCoreApplicationNotFoundException extends RuntimeException {

    private final RecruitCoreApplicationErrorCode errorCode = RecruitCoreApplicationErrorCode.APPLICATION_NOT_FOUND;

    public RecruitCoreApplicationNotFoundException() {
        super(RecruitCoreApplicationErrorCode.APPLICATION_NOT_FOUND.getMessage());
    }
}
