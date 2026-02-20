package inha.gdgoc.domain.admin.recruit.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecruitMemberMemoOpeningNotificationRequest(
        @NotBlank(message = "메일 제목은 필수입니다.")
        @Size(max = 200, message = "메일 제목은 200자 이하여야 합니다.")
        String subject,

        @NotBlank(message = "메일 본문은 필수입니다.")
        @Size(max = 5000, message = "메일 본문은 5000자 이하여야 합니다.")
        String body
) {
}
