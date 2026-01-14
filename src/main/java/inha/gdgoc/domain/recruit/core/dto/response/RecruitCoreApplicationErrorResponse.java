package inha.gdgoc.domain.recruit.core.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecruitCoreApplicationErrorResponse(
    String code,
    String message,
    Details details
) {

    public static RecruitCoreApplicationErrorResponse of(
        String code,
        String message
    ) {
        return new RecruitCoreApplicationErrorResponse(code, message, null);
    }

    public static RecruitCoreApplicationErrorResponse of(
        String code,
        String message,
        String session,
        Long applicationId
    ) {
        return new RecruitCoreApplicationErrorResponse(code, message, new Details(session, applicationId));
    }

    public record Details(String session, Long applicationId) {}
}
