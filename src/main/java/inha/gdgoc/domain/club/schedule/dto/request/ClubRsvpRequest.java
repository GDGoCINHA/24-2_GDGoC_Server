package inha.gdgoc.domain.club.schedule.dto.request;

import inha.gdgoc.domain.club.schedule.enums.ClubRsvp;
import jakarta.validation.constraints.NotNull;

/** 참석 예정·불참 예정. 실제 출석과 무관하다. */
public record ClubRsvpRequest(@NotNull ClubRsvp response) {}
