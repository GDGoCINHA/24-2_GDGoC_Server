package inha.gdgoc.domain.club.completion.dto;

import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import inha.gdgoc.domain.club.completion.enums.ClubCompletionWarning;
import inha.gdgoc.domain.club.completion.enums.ClubGoalStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * 완주 계산 결과. 참고용이다 — 최종 완주는 운영진이 확정한다.
 *
 * @param period 활동 기간. null 이면 주차 계산을 하지 않았다 ({@code weeks} 는 빈 목록)
 * @param pendingActivityCount 아직 검토하지 않은(PENDING) 기록 수 — 운영진이 확정 전에 보게
 * @param outOfPeriodActivityCount 활동 기간 밖 기록 수 — 계산에서 뺐다
 * @param eligible 쉬는 주를 뺀 모든 주 충족 && 팀원 4명 이상 && 목표 ACHIEVED
 */
public record CompletionResult(
    Period period,
    List<Week> weeks,
    int memberCount,
    boolean memberCountSatisfied,
    ClubGoalStatus goalStatus,
    int pendingActivityCount,
    int outOfPeriodActivityCount,
    boolean eligible,
    List<ClubCompletionWarning> warnings) {

  public record Period(LocalDate startDate, LocalDate endDate) {}

  /**
   * 한 주 (월요일 시작).
   *
   * @param rest 쉬는 주면 true — 판정에서 빠지며 {@code satisfied} 는 null
   * @param satisfied 인정 활동이 1회 이상이면 true. 아직 끝나지 않은 주에 인정 활동이 없으면 null (진행 중)
   */
  public record Week(
      LocalDate weekStart, boolean rest, List<WeekActivity> activities, Boolean satisfied) {}

  /**
   * 그 주의 활동 기록.
   *
   * @param required 필요 참석 인원 = ceil(roster × 참석 비율)
   * @param counted 인증 완료(APPROVED)이고 필요 인원을 채워 인정 활동인지
   */
  public record WeekActivity(
      Long activityId,
      LocalDate date,
      ClubActivityStatus status,
      int attended,
      int roster,
      int required,
      boolean counted) {}
}
