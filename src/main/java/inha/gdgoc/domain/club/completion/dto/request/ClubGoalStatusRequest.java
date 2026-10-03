package inha.gdgoc.domain.club.completion.dto.request;

import inha.gdgoc.domain.club.completion.enums.ClubGoalStatus;
import jakarta.validation.constraints.NotNull;

/** 운영진의 목표 판정. ACHIEVED 또는 NOT_ACHIEVED. */
public record ClubGoalStatusRequest(@NotNull ClubGoalStatus status) {}
