package inha.gdgoc.domain.recruit.core.exception;

import lombok.Getter;

@Getter
public class RecruitCoreAlreadyAppliedException extends RuntimeException {

    private final RecruitCoreApplicationErrorCode errorCode;
    private final String session;
    private final Long applicationId;

    public RecruitCoreAlreadyAppliedException(String session, Long applicationId) {
        super(RecruitCoreApplicationErrorCode.ALREADY_APPLIED.getMessage());
        this.errorCode = RecruitCoreApplicationErrorCode.ALREADY_APPLIED;
        this.session = session;
        this.applicationId = applicationId;
    }
}
