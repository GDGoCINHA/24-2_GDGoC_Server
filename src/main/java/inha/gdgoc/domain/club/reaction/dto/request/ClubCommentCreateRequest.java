package inha.gdgoc.domain.club.reaction.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClubCommentCreateRequest(@NotBlank @Size(max = 1000) String content) {}
