package inha.gdgoc.domain.admin.recruit.core.dto.response;

import inha.gdgoc.domain.recruit.core.entity.RecruitCoreApplication;
import inha.gdgoc.domain.recruit.core.enums.RecruitCoreResultStatus;
import java.time.Instant;

public record RecruitCoreApplicantSummaryResponse(
    Long applicationId,
    String name,
    String studentId,
    String major,
    String team,
    RecruitCoreResultStatus resultStatus,
    String session,
    Instant createdAt
) {

    public static RecruitCoreApplicantSummaryResponse from(RecruitCoreApplication entity) {
        return new RecruitCoreApplicantSummaryResponse(
            entity.getId(),
            entity.getName(),
            entity.getStudentId(),
            entity.getMajor(),
            entity.getTeam(),
            entity.getResultStatus(),
            entity.getSession(),
            entity.getCreatedAt()
        );
    }
}
