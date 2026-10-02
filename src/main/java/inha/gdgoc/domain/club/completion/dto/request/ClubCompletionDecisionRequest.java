package inha.gdgoc.domain.club.completion.dto.request;

import inha.gdgoc.domain.club.completion.enums.ClubCompletionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 운영진의 완주 확정. COMPLETED 또는 FAILED. 계산값과 무관하게 확정할 수 있다. */
public record ClubCompletionDecisionRequest(
    @NotNull ClubCompletionStatus status, @Size(max = 1000) String memo) {}
