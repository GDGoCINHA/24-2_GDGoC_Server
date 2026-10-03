package inha.gdgoc.domain.club.leader.dto.request;

import jakarta.validation.constraints.NotNull;

public record ClubLeaderGrantRequest(@NotNull Long userId) {}
