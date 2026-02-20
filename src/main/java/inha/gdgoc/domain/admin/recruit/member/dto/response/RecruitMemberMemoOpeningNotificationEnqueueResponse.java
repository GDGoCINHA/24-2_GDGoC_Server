package inha.gdgoc.domain.admin.recruit.member.dto.response;

import inha.gdgoc.domain.recruit.member.notification.service.RecruitMemberMemoNotificationEnqueueResult;

public record RecruitMemberMemoOpeningNotificationEnqueueResponse(
        String semester,
        int distinctTargetCount,
        int enqueuedCount,
        int alreadyProcessedCount
) {
    public static RecruitMemberMemoOpeningNotificationEnqueueResponse from(
            RecruitMemberMemoNotificationEnqueueResult result
    ) {
        return new RecruitMemberMemoOpeningNotificationEnqueueResponse(
                result.semester(),
                result.distinctTargetCount(),
                result.enqueuedCount(),
                result.alreadyProcessedCount()
        );
    }
}
