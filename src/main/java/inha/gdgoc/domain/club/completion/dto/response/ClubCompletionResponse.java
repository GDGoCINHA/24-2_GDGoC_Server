package inha.gdgoc.domain.club.completion.dto.response;

import inha.gdgoc.domain.club.completion.dto.CompletionResult;
import inha.gdgoc.domain.club.completion.dto.CompletionResult.Period;
import inha.gdgoc.domain.club.completion.dto.CompletionResult.Week;
import inha.gdgoc.domain.club.completion.entity.ClubCompletion;
import inha.gdgoc.domain.club.completion.enums.ClubCompletionStatus;
import inha.gdgoc.domain.club.completion.enums.ClubCompletionWarning;
import inha.gdgoc.domain.club.completion.enums.ClubGoalStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * {@code GET /clubs/{id}/completion}. 계산 결과({@link CompletionResult}) + 목표 + 확정 상태.
 *
 * <p>{@code club_completion} 행이 아직 없으면 목표 항목은 null, 상태는 NOT_SUBMITTED·IN_PROGRESS 로 응답한다.
 */
public record ClubCompletionResponse(
    // 계산
    Period period,
    List<Week> weeks,
    List<LocalDate> restWeeks,
    int memberCount,
    boolean memberCountSatisfied,
    ClubGoalStatus goalStatus,
    int pendingActivityCount,
    int outOfPeriodActivityCount,
    boolean eligible,
    List<ClubCompletionWarning> warnings,
    // 목표
    String goal,
    String goalCriteria,
    String goalResult,
    List<String> goalEvidenceUrls,
    // 확정
    ClubCompletionStatus completionStatus,
    String completionMemo,
    Instant confirmedAt) {

  /** @param completion 행이 없으면 null */
  public static ClubCompletionResponse of(
      CompletionResult r, List<LocalDate> restWeeks, ClubCompletion completion) {
    boolean has = completion != null;
    return new ClubCompletionResponse(
        r.period(),
        r.weeks(),
        restWeeks,
        r.memberCount(),
        r.memberCountSatisfied(),
        r.goalStatus(),
        r.pendingActivityCount(),
        r.outOfPeriodActivityCount(),
        r.eligible(),
        r.warnings(),
        has ? completion.getGoal() : null,
        has ? completion.getGoalCriteria() : null,
        has ? completion.getGoalResult() : null,
        has && completion.getGoalEvidenceUrls() != null ? completion.getGoalEvidenceUrls() : List.of(),
        has ? completion.getCompletionStatus() : ClubCompletionStatus.IN_PROGRESS,
        has ? completion.getCompletionMemo() : null,
        has ? completion.getConfirmedAt() : null);
  }
}
