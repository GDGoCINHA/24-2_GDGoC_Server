package inha.gdgoc.domain.club.member.dto.request;

import jakarta.validation.constraints.Size;

public record ClubApplyRequest(@Size(max = 500) String message) {}
