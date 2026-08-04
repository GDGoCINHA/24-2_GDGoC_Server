package inha.gdgoc.domain.recruit.core.dto.response;

import inha.gdgoc.domain.recruit.core.entity.RecruitCoreApplication;
import inha.gdgoc.domain.recruit.core.enums.RecruitCoreResultStatus;
import java.time.Instant;
import java.util.List;

public record RecruitCoreApplicantDetailResponse(
    Long applicationId,
    String session,
    RecruitCoreApplicationSnapshotResponse snapshot,
    String team,
    String motivation,
    String wish,
    String strengths,
    String pledge,
    List<String> fileUrls,
    RecruitCoreResultStatus resultStatus,
    RecruitCoreApplicationReviewResponse review,
    Instant createdAt,
    Instant updatedAt
) {

    public static RecruitCoreApplicantDetailResponse from(RecruitCoreApplication entity) {
        return from(entity, entity.getFileUrls());
    }

    public static RecruitCoreApplicantDetailResponse from(RecruitCoreApplication entity, List<String> fileUrls) {
        return new RecruitCoreApplicantDetailResponse(
            entity.getId(),
            entity.getSession(),
            RecruitCoreApplicationSnapshotResponse.from(entity),
            entity.getTeam(),
            entity.getMotivation(),
            entity.getWish(),
            entity.getStrengths(),
            entity.getPledge(),
            fileUrls,
            entity.getResultStatus(),
            RecruitCoreApplicationReviewResponse.from(entity),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
