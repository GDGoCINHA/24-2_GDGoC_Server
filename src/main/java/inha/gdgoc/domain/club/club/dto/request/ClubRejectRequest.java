package inha.gdgoc.domain.club.club.dto.request;

import jakarta.validation.constraints.Size;

public record ClubRejectRequest(@Size(max = 500) String reason) {}
