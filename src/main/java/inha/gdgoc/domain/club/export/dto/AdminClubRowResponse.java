package inha.gdgoc.domain.club.export.dto;

import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.enums.ClubStatus;
import inha.gdgoc.domain.club.completion.dto.CompletionResult.Week;
import inha.gdgoc.domain.club.completion.dto.response.ClubCompletionResponse;
import inha.gdgoc.domain.club.completion.enums.ClubCompletionStatus;
import inha.gdgoc.domain.club.completion.enums.ClubCompletionWarning;
import inha.gdgoc.domain.club.completion.enums.ClubGoalStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * 운영진 현황 표 한 줄. 팀 화면의 완주 계산과 같은 값에서 만든다.
 *
 * @param satisfiedWeeks 쉬는 주를 뺀 주 중 충족한 주
 * @param targetWeeks 쉬는 주를 뺀 주 (활동 기간 전체 — 아직 오지 않은 주 포함)
 * @param restWeeks 활동 기간 안의 쉬는 주
 * @param pendingReviewCount 검토 대기(PENDING) 기록 수
 */
public record AdminClubRowResponse(
    Long clubId,
    String name,
    Long leaderId,
    String leaderName,
    ClubStatus status,
    LocalDate startDate,
    LocalDate endDate,
    int memberCount,
    Integer capacity,
    ClubGoalStatus goalStatus,
    int satisfiedWeeks,
    int targetWeeks,
    int restWeeks,
    int pendingReviewCount,
    List<ClubCompletionWarning> warnings,
    boolean eligible,
    ClubCompletionStatus completionStatus) {

  public static AdminClubRowResponse of(Club club, ClubCompletionResponse c) {
    List<Week> target = c.weeks().stream().filter(w -> !w.rest()).toList();
    return new AdminClubRowResponse(
        club.getId(),
        club.getName(),
        club.getLeader().getId(),
        club.getLeader().getName(),
        club.getStatus(),
        club.getStartDate(),
        club.getEndDate(),
        c.memberCount(),
        club.getCapacity(),
        c.goalStatus(),
        (int) target.stream().filter(w -> Boolean.TRUE.equals(w.satisfied())).count(),
        target.size(),
        c.weeks().size() - target.size(),
        c.pendingActivityCount(),
        c.warnings(),
        c.eligible(),
        c.completionStatus());
  }
}
