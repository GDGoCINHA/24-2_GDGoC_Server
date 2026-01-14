package inha.gdgoc.domain.recruit.core.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RecruitCoreApplicationCreateRequest(
    @Valid @NotNull RecruitCoreApplicationSnapshotRequest snapshot,
    @NotBlank String team,
    @NotBlank String motivation,
    @NotBlank String wish,
    @NotBlank String strengths,
    @NotBlank String pledge,
    @NotNull @Size(min = 0) List<@NotBlank String> fileUrls
) {

    public record RecruitCoreApplicationSnapshotRequest(
        @NotBlank String name,
        @NotBlank String studentId,
        @NotBlank String phone,
        @NotBlank String major,
        @NotBlank @Email String email
    ) {
    }
}
