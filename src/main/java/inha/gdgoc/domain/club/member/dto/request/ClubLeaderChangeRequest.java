package inha.gdgoc.domain.club.member.dto.request;

import jakarta.validation.constraints.NotNull;

public record ClubLeaderChangeRequest(@NotNull Long userId) {}
