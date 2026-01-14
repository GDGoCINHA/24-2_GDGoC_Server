package inha.gdgoc.domain.recruit.core.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import inha.gdgoc.domain.recruit.core.entity.RecruitCoreApplication;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecruitCoreApplicationReviewResponse(
    Instant reviewedAt,
    Long reviewedBy,
    String resultNote
) {

    public static RecruitCoreApplicationReviewResponse from(RecruitCoreApplication application) {
        if (application.getReviewedAt() == null
            && application.getReviewedBy() == null
            && application.getResultNote() == null) {
            return new RecruitCoreApplicationReviewResponse(null, null, null);
        }
        return new RecruitCoreApplicationReviewResponse(
            application.getReviewedAt(),
            application.getReviewedBy(),
            application.getResultNote()
        );
    }
}
