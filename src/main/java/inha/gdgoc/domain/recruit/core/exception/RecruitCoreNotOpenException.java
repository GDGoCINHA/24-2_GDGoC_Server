package inha.gdgoc.domain.recruit.core.exception;

import java.time.Instant;
import lombok.Getter;

@Getter
public class RecruitCoreNotOpenException extends RuntimeException {

    private final RecruitCoreApplicationErrorCode errorCode;
    private final Instant openAt;

    public RecruitCoreNotOpenException(Instant openAt) {
        super(RecruitCoreApplicationErrorCode.RECRUITMENT_NOT_OPEN.getMessage());
        this.errorCode = RecruitCoreApplicationErrorCode.RECRUITMENT_NOT_OPEN;
        this.openAt = openAt;
    }
}
