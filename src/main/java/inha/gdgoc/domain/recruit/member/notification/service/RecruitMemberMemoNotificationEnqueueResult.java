package inha.gdgoc.domain.recruit.member.notification.service;

public record RecruitMemberMemoNotificationEnqueueResult(
        String semester,
        int distinctTargetCount,
        int enqueuedCount,
        int alreadyProcessedCount
) {
}
