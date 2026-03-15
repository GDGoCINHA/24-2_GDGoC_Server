package inha.gdgoc.domain.recruit.core.exception;

import java.time.Instant;
import lombok.Getter;

@Getter
public class RecruitCoreClosedException extends RuntimeException {

    private final RecruitCoreApplicationErrorCode errorCode;
    private final Instant deadline;

    public RecruitCoreClosedException(Instant deadline) {
        super(RecruitCoreApplicationErrorCode.RECRUITMENT_CLOSED.getMessage());
        this.errorCode = RecruitCoreApplicationErrorCode.RECRUITMENT_CLOSED;
        this.deadline = deadline;
    }
}
