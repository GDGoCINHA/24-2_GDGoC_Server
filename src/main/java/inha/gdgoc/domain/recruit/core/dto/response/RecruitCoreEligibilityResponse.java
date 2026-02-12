package inha.gdgoc.domain.recruit.core.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecruitCoreEligibilityResponse(
    boolean eligible,
    String session,
    String reason,
    Long applicationId
) {

    public static RecruitCoreEligibilityResponse eligible(String session) {
        return new RecruitCoreEligibilityResponse(true, session, null, null);
    }

    public static RecruitCoreEligibilityResponse ineligible(String session, String reason, Long applicationId) {
        return new RecruitCoreEligibilityResponse(false, session, reason, applicationId);
    }
}
