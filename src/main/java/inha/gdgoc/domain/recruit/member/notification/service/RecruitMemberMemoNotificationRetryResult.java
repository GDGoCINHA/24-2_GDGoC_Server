package inha.gdgoc.domain.recruit.member.notification.service;

public record RecruitMemberMemoNotificationRetryResult(
        String semester,
        int retriedCount
) {
}
