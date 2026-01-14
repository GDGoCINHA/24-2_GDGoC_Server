package inha.gdgoc.domain.admin.recruit.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecruitCoreApplicationAcceptRequest(
    @NotBlank String resultNote,
    @NotNull Boolean overwriteTeamIfExists
) {
}
