package inha.gdgoc.domain.admin.recruit.member.dto.response;

public record RecruitMemberMemoFailedRetryResponse(
        String semester,
        int retriedCount
) {
}
