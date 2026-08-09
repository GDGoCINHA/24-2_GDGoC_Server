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

    // fileUrls 는 반드시 호출부에서 S3 URL 로 변환해 넘긴다.
    // 엔티티에 저장된 값은 S3 키이므로, 그대로 내려주면 클라이언트에서 링크가 열리지 않는다.
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
