package inha.gdgoc.domain.club.leader.dto.request;

import inha.gdgoc.domain.club.club.enums.ClubCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClubOpenRequestCreateRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull ClubCategory category,
    @NotBlank @Size(max = 200) String summary,
    String goal) {}
