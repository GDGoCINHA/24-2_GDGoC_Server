package inha.gdgoc.domain.recruit.core.dto.response;

import inha.gdgoc.domain.recruit.core.entity.RecruitCoreApplication;
import inha.gdgoc.domain.recruit.core.enums.RecruitCoreResultStatus;
import java.time.Instant;

public record RecruitCoreMyApplicationResponse(
    Long applicationId,
    String session,
    String team,
    RecruitCoreResultStatus resultStatus,
    Instant createdAt,
    Instant updatedAt
) {

    public static RecruitCoreMyApplicationResponse from(RecruitCoreApplication application) {
        return new RecruitCoreMyApplicationResponse(
            application.getId(),
            application.getSession(),
            application.getTeam(),
            application.getResultStatus(),
            application.getCreatedAt(),
            application.getUpdatedAt()
        );
    }
}
