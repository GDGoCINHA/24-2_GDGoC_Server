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
        return from(entity, fileUrls, true);
    }

    /**
     * {@code includeReview} 가 false 면 검토 정보를 뺀다.
     *
     * <p>지원자 본인도 이 DTO 로 자기 지원서를 본다(마이페이지). 검토자 ID 와 내부 메모는
     * 합격/불합격 판단 근거라 지원자에게 보일 값이 아니다. 화면에서 안 그리는 것만으로는
     * 부족하다 — 응답에 실려 나가면 개발자 도구로 그대로 읽힌다.
     */
    public static RecruitCoreApplicantDetailResponse from(
        RecruitCoreApplication entity,
        List<String> fileUrls,
        boolean includeReview
    ) {
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
            includeReview ? RecruitCoreApplicationReviewResponse.from(entity) : null,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
