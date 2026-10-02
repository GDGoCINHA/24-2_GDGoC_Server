package inha.gdgoc.domain.club.completion.service;

import static java.time.temporal.TemporalAdjusters.previousOrSame;

import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import inha.gdgoc.domain.club.completion.dto.CompletionInput;
import inha.gdgoc.domain.club.completion.dto.CompletionInput.Activity;
import inha.gdgoc.domain.club.completion.dto.CompletionResult;
import inha.gdgoc.domain.club.completion.dto.CompletionResult.Period;
import inha.gdgoc.domain.club.completion.dto.CompletionResult.Week;
import inha.gdgoc.domain.club.completion.dto.CompletionResult.WeekActivity;
import inha.gdgoc.domain.club.completion.enums.ClubCompletionWarning;
import inha.gdgoc.domain.club.completion.enums.ClubGoalStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 완주 기준 충족 여부 계산. 입력만으로 결정되는 순수 함수다 — 운영진이 이 결과를 믿고 확정하므로 테스트로 고정한다.
 *
 * <p>규칙 (공통 계약 「확정 규칙」):
 *
 * <ul>
 *   <li>주차는 월요일 시작, 활동일 기준. 기간 시작·끝이 주 중간이면 그 주도 포함한다.
 *   <li>필요 참석 = ceil(명단 수 × 참석 비율). 인증 완료(APPROVED)이고 필요 인원을 채운 기록만 인정 활동이다.
 *   <li>쉬는 주는 판정에서 뺀다.
 *   <li>충족 = 쉬는 주를 뺀 모든 주에 인정 활동 1회 이상 + 팀원 4명 이상 + 목표 ACHIEVED.
 * </ul>
 */
public final class CompletionCalculator {

  static final int MIN_MEMBERS = 4;

  private CompletionCalculator() {}

  public static CompletionResult calculate(CompletionInput in) {
    int pending =
        (int)
            in.activities().stream()
                .filter(a -> a.status() == ClubActivityStatus.PENDING)
                .count();
    boolean membersOk = in.memberCount() >= MIN_MEMBERS;

    List<ClubCompletionWarning> warnings = new ArrayList<>();
    boolean periodSet = in.startDate() != null && in.endDate() != null;

    List<Week> weeks = List.of();
    int outOfPeriod = 0;
    if (!periodSet) {
      warnings.add(ClubCompletionWarning.PERIOD_NOT_SET);
    } else {
      LocalDate firstWeek = weekStartOf(in.startDate());
      LocalDate lastWeek = weekStartOf(in.endDate());
      LocalDate thisWeek = weekStartOf(in.today());

      // 「걸친 주 포함」이므로 기간 밖 판정도 날짜가 아니라 주 단위로 한다.
      Map<LocalDate, List<Activity>> byWeek = new TreeMap<>();
      for (Activity a : in.activities()) {
        LocalDate week = weekStartOf(a.date());
        if (week.isBefore(firstWeek) || week.isAfter(lastWeek)) {
          outOfPeriod++;
        } else {
          byWeek.computeIfAbsent(week, k -> new ArrayList<>()).add(a);
        }
      }

      weeks = new ArrayList<>();
      for (LocalDate w = firstWeek; !w.isAfter(lastWeek); w = w.plusWeeks(1)) {
        weeks.add(week(w, byWeek.getOrDefault(w, List.of()), in, thisWeek));
      }

      boolean missed = weeks.stream().anyMatch(w -> Boolean.FALSE.equals(w.satisfied()));
      if (missed) {
        warnings.add(ClubCompletionWarning.WEEK_MISSED);
      }
      if (outOfPeriod > 0) {
        warnings.add(ClubCompletionWarning.OUT_OF_PERIOD);
      }
    }

    if (!membersOk) {
      warnings.add(ClubCompletionWarning.MEMBERS_UNDER_4);
    }
    if (!in.goalSet()) {
      warnings.add(ClubCompletionWarning.GOAL_NOT_SET);
    }
    if (in.capacity() != null && in.memberCount() > in.capacity()) {
      warnings.add(ClubCompletionWarning.OVER_CAPACITY);
    }

    // 기간이 뒤집혀 주가 하나도 없으면 「모든 주 충족」이 공허하게 참이 되므로 막는다.
    boolean allWeeksOk =
        !weeks.isEmpty()
            && weeks.stream().filter(w -> !w.rest()).allMatch(w -> Boolean.TRUE.equals(w.satisfied()));
    boolean eligible =
        periodSet && allWeeksOk && membersOk && in.goalStatus() == ClubGoalStatus.ACHIEVED;

    return new CompletionResult(
        periodSet ? new Period(in.startDate(), in.endDate()) : null,
        weeks,
        in.memberCount(),
        membersOk,
        in.goalStatus(),
        pending,
        outOfPeriod,
        eligible,
        warnings.stream().sorted(Comparator.naturalOrder()).toList());
  }

  /** 필요 참석 인원 = ceil(명단 × 비율). 부동소수 오차를 피하려고 BigDecimal 로 센다 (0.1 × 30 같은 경우). */
  public static int required(int roster, BigDecimal ratio) {
    return BigDecimal.valueOf(roster).multiply(ratio).setScale(0, RoundingMode.CEILING).intValueExact();
  }

  /**
   * 필요 인원을 채웠는가. 명단이 비어 있으면(0/0) 채운 것으로 보지 않는다 — ceil(0 × 비율) = 0 이라 그대로 두면 아무도 없는 기록이
   * 인정 활동이 된다.
   */
  public static boolean meetsRequired(int roster, int attended, int required) {
    return roster > 0 && attended >= required;
  }

  /** 그 날짜가 속한 주의 월요일. 쉬는 주 저장 시에도 이것으로 맞춘다. */
  public static LocalDate weekStartOf(LocalDate date) {
    return date.with(previousOrSame(DayOfWeek.MONDAY));
  }

  private static Week week(
      LocalDate weekStart, List<Activity> activities, CompletionInput in, LocalDate thisWeek) {
    List<WeekActivity> rows =
        activities.stream()
            .sorted(Comparator.comparing(Activity::date))
            .map(a -> toRow(a, in.attendanceRatio()))
            .toList();

    if (in.restWeeks().contains(weekStart)) {
      return new Week(weekStart, true, rows, null);
    }
    boolean counted = rows.stream().anyMatch(WeekActivity::counted);
    Boolean satisfied;
    if (counted) {
      satisfied = true;
    } else if (weekStart.isBefore(thisWeek)) {
      satisfied = false;
    } else {
      satisfied = null; // 이번 주 또는 아직 오지 않은 주 — 진행 중
    }
    return new Week(weekStart, false, rows, satisfied);
  }

  private static WeekActivity toRow(Activity a, BigDecimal ratio) {
    int required = required(a.roster(), ratio);
    boolean counted =
        a.status() == ClubActivityStatus.APPROVED
            && meetsRequired(a.roster(), a.attended(), required);
    return new WeekActivity(
        a.activityId(), a.date(), a.status(), a.attended(), a.roster(), required, counted);
  }
}
