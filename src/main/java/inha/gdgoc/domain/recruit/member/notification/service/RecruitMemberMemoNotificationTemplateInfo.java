package inha.gdgoc.domain.recruit.member.notification.service;

public record RecruitMemberMemoNotificationTemplateInfo(
        String semester,
        String defaultSubject,
        String defaultBody,
        String lastSubject,
        String lastBody
) {
}
