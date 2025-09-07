package inha.gdgoc.domain.core.recruit.dto.response;

import inha.gdgoc.domain.core.recruit.entity.CoreRecruitApplication;
import java.time.Instant;

public record CoreRecruitApplicantSummaryResponse(
    Long id,
    String name,
    String studentId,
    String major,
    String email,
    String phone,
    String team,
    Instant createdAt
) {

    public static CoreRecruitApplicantSummaryResponse from(CoreRecruitApplication entity) {
        return new CoreRecruitApplicantSummaryResponse(
            entity.getId(),
            entity.getName(),
            entity.getStudentId(),
            entity.getMajor(),
            entity.getEmail(),
            entity.getPhone(),
            entity.getTeam(),
            entity.getCreatedAt()
        );
    }
}


