package inha.gdgoc.domain.club.completion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClubGoalRequest(
    @NotBlank @Size(max = 2000) String goal, @Size(max = 2000) String goalCriteria) {}
