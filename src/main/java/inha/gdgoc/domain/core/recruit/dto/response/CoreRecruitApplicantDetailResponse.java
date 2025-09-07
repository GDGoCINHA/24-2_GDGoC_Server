package inha.gdgoc.domain.core.recruit.dto.response;

import inha.gdgoc.domain.core.recruit.entity.CoreRecruitApplication;
import java.time.Instant;
import java.util.List;

public record CoreRecruitApplicantDetailResponse(
    Long id,
    String name,
    String studentId,
    String phone,
    String major,
    String email,
    String team,
    String motivation,
    String wish,
    String strengths,
    String pledge,
    List<String> fileUrls,
    Instant createdAt,
    Instant updatedAt
) {

    public static CoreRecruitApplicantDetailResponse from(CoreRecruitApplication entity) {
        return new CoreRecruitApplicantDetailResponse(
            entity.getId(),
            entity.getName(),
            entity.getStudentId(),
            entity.getPhone(),
            entity.getMajor(),
            entity.getEmail(),
            entity.getTeam(),
            entity.getMotivation(),
            entity.getWish(),
            entity.getStrengths(),
            entity.getPledge(),
            entity.getFileUrls(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}


