package inha.gdgoc.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @NotBlank
        @Size(min = 1, max = 30)
        String name,

        @NotBlank
        String major,

        @NotBlank
        String phoneNumber
) {
}
