package inha.gdgoc.domain.club.leader.dto.request;

import jakarta.validation.constraints.Size;

public record ClubOpenRequestRejectRequest(@Size(max = 500) String reason) {}
