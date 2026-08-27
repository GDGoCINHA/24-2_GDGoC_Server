package inha.gdgoc.domain.eventapplication.dto.request;

import jakarta.validation.constraints.NotBlank;

/** QR 에 담겨 있던 토큰. 60 초마다 바뀐다. */
public record CheckinRequest(@NotBlank String token) {}
