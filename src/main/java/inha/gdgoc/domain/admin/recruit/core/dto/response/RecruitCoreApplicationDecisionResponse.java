package inha.gdgoc.domain.admin.recruit.core.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import inha.gdgoc.domain.recruit.core.entity.RecruitCoreApplication;
import inha.gdgoc.domain.recruit.core.enums.RecruitCoreResultStatus;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecruitCoreApplicationDecisionResponse(
    Long applicationId,
    RecruitCoreResultStatus resultStatus,
    Instant reviewedAt,
    Long reviewedBy,
    UserUpdated userUpdated
) {

    public static RecruitCoreApplicationDecisionResponse accepted(
        RecruitCoreApplication application,
        UserRole userRole,
        TeamType team
    ) {
        return new RecruitCoreApplicationDecisionResponse(
            application.getId(),
            application.getResultStatus(),
            application.getReviewedAt(),
            application.getReviewedBy(),
            new UserUpdated(userRole, team)
        );
    }

    public static RecruitCoreApplicationDecisionResponse rejected(RecruitCoreApplication application) {
        return new RecruitCoreApplicationDecisionResponse(
            application.getId(),
            application.getResultStatus(),
            application.getReviewedAt(),
            application.getReviewedBy(),
            null
        );
    }

    public record UserUpdated(UserRole userRole, TeamType team) {}
}
