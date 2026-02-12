package inha.gdgoc.domain.admin.recruit.core.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RecruitCoreApplicationRejectRequest(
    @NotBlank String resultNote
) {
}
