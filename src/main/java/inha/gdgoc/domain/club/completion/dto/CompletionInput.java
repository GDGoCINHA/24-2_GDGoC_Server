package inha.gdgoc.domain.club.completion.dto;

import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import inha.gdgoc.domain.club.completion.enums.ClubGoalStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 완주 계산의 입력. DB 를 모르는 값만 담아 {@code CompletionCalculator} 를 순수 함수로 둔다.
 *
 * @param startDate 소모임 활동 기간 시작 (null 이면 기간 미설정)
 * @param endDate 소모임 활동 기간 끝 (null 이면 기간 미설정)
 * @param restWeeks 쉬는 주 — 각 주의 월요일
 * @param attendanceRatio 기수 참석 비율 (예: 0.50)
 * @param activities 이 소모임의 활동 기록 전부 (상태 무관)
 * @param memberCount 지금 팀원 수 (이끔이 포함)
 * @param capacity 모집 정원 (null 이면 제한 없음)
 * @param goalSet 목표가 등록돼 있는지
 * @param goalStatus 목표 상태
 * @param today 오늘 (한국 날짜) — 아직 오지 않은 주를 가르는 기준
 */
public record CompletionInput(
    LocalDate startDate,
    LocalDate endDate,
    Set<LocalDate> restWeeks,
    BigDecimal attendanceRatio,
    List<Activity> activities,
    int memberCount,
    Integer capacity,
    boolean goalSet,
    ClubGoalStatus goalStatus,
    LocalDate today) {

  public CompletionInput {
    restWeeks = restWeeks == null ? Set.of() : Set.copyOf(restWeeks);
    activities = activities == null ? List.of() : List.copyOf(activities);
  }

  /**
   * 활동 기록 한 건.
   *
   * @param roster 활동일 당시 명단 수 (출석 행 수) — 참석 비율의 분모
   * @param attended 실제 출석 수
   */
  public record Activity(
      Long activityId, LocalDate date, ClubActivityStatus status, int roster, int attended) {}
}
