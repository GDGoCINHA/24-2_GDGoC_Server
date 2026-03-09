package inha.gdgoc.domain.admin.game.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import java.util.List;

public record MbtiTeamMatchRequest(
        @NotEmpty List<@Valid Candidate> candidates,
        @Min(2) @Max(10) Integer teamSize
) {
    public int resolvedTeamSize() {
        return teamSize == null ? 4 : teamSize;
    }

    public record Candidate(
            @NotBlank String name,
            @NotBlank String studentId
    ) {
    }
}
