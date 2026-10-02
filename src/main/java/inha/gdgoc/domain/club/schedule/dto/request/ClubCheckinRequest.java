package inha.gdgoc.domain.club.schedule.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** QR 에 담긴 토큰. 어느 일정인지는 토큰 안에 들어 있다. */
public record ClubCheckinRequest(@NotBlank @Size(max = 100) String token) {}
