package inha.gdgoc.domain.recruit.core.dto.response;

import inha.gdgoc.domain.recruit.core.entity.RecruitCoreApplication;
import inha.gdgoc.domain.recruit.core.enums.RecruitCoreResultStatus;
import java.time.Instant;

public record RecruitCoreApplicationCreateResponse(
    Long applicationId,
    String session,
    RecruitCoreResultStatus resultStatus,
    Instant submittedAt
) {

    public static RecruitCoreApplicationCreateResponse from(RecruitCoreApplication application) {
        return new RecruitCoreApplicationCreateResponse(
            application.getId(),
            application.getSession(),
            application.getResultStatus(),
            application.getCreatedAt()
        );
    }
}
