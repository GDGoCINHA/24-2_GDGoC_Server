package inha.gdgoc.domain.admin.recruit.member.dto.response;

public record RecruitMemberMemoNotificationTemplateResponse(
        String semester,
        String defaultSubject,
        String defaultBody,
        String lastSubject,
        String lastBody
) {
}
