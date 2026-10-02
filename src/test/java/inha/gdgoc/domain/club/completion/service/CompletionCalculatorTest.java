package inha.gdgoc.domain.club.completion.service;

import static inha.gdgoc.domain.club.activity.enums.ClubActivityStatus.APPROVED;
import static inha.gdgoc.domain.club.activity.enums.ClubActivityStatus.PENDING;
import static inha.gdgoc.domain.club.activity.enums.ClubActivityStatus.REVISION_REQUESTED;
import static inha.gdgoc.domain.club.completion.enums.ClubCompletionWarning.GOAL_NOT_SET;
import static inha.gdgoc.domain.club.completion.enums.ClubCompletionWarning.MEMBERS_UNDER_4;
import static inha.gdgoc.domain.club.completion.enums.ClubCompletionWarning.OUT_OF_PERIOD;
import static inha.gdgoc.domain.club.completion.enums.ClubCompletionWarning.OVER_CAPACITY;
import static inha.gdgoc.domain.club.completion.enums.ClubCompletionWarning.PERIOD_NOT_SET;
import static inha.gdgoc.domain.club.completion.enums.ClubCompletionWarning.WEEK_MISSED;
import static org.assertj.core.api.Assertions.assertThat;

import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import inha.gdgoc.domain.club.completion.dto.CompletionInput;
import inha.gdgoc.domain.club.completion.dto.CompletionInput.Activity;
import inha.gdgoc.domain.club.completion.dto.CompletionResult;
import inha.gdgoc.domain.club.completion.dto.CompletionResult.Week;
import inha.gdgoc.domain.club.completion.enums.ClubGoalStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** 완주 계산. 운영진이 이 결과를 믿고 확정하므로 경계를 하나씩 고정한다. */
class CompletionCalculatorTest {

  // 2026-10-05 은 월요일. 기간은 3주: 10/05 ~ 10/25(일).
  private static final LocalDate W1 = LocalDate.of(2026, 10, 5);
  private static final LocalDate W2 = W1.plusWeeks(1);
  private static final LocalDate W3 = W1.plusWeeks(2);
  private static final LocalDate END = W3.plusDays(6);
  private static final LocalDate AFTER_END = END.plusDays(1);
  private static final BigDecimal HALF = new BigDecimal("0.50");

  private final AtomicLong ids = new AtomicLong();

  private Activity act(LocalDate date, ClubActivityStatus status, int roster, int attended) {
    return new Activity(ids.incrementAndGet(), date, status, roster, attended);
  }

  /** 세 주 모두 인정 활동이 있는 기본 활동 목록. */
  private List<Activity> fullWeeks() {
    return List.of(
        act(W1.plusDays(2), APPROVED, 5, 3),
        act(W2.plusDays(2), APPROVED, 5, 3),
        act(W3.plusDays(2), APPROVED, 5, 3));
  }

  /** 기본값에서 한두 가지만 바꿔 경계를 드러내려는 테스트 전용 빌더. */
  private static final class InputBuilder {
    private LocalDate start;
    private LocalDate end;
    private Set<LocalDate> rest = Set.of();
    private BigDecimal ratio;
    private List<Activity> activities;
    private int members;
    private Integer capacity;
    private boolean goalSet = true;
    private ClubGoalStatus goal;
    private LocalDate today;

    InputBuilder period(LocalDate start, LocalDate end) {
      this.start = start;
      this.end = end;
      return this;
    }

    InputBuilder rest(LocalDate... weeks) {
      this.rest = Set.of(weeks);
      return this;
    }

    InputBuilder ratio(BigDecimal ratio) {
      this.ratio = ratio;
      return this;
    }

    InputBuilder activities(List<Activity> activities) {
      this.activities = activities;
      return this;
    }

    InputBuilder members(int members) {
      this.members = members;
      return this;
    }

    InputBuilder capacity(Integer capacity) {
      this.capacity = capacity;
      return this;
    }

    InputBuilder goalSet(boolean goalSet) {
      this.goalSet = goalSet;
      return this;
    }

    InputBuilder goal(ClubGoalStatus goal) {
      this.goal = goal;
      return this;
    }

    InputBuilder today(LocalDate today) {
      this.today = today;
      return this;
    }

    CompletionInput build() {
      return new CompletionInput(
          start, end, rest, ratio, activities, members, capacity, goalSet, goal, today);
    }
  }

  private InputBuilder base() {
    return new InputBuilder()
        .period(W1, END)
        .ratio(HALF)
        .activities(fullWeeks())
        .members(4)
        .goal(ClubGoalStatus.ACHIEVED)
        .today(AFTER_END);
  }

  private static Week week(CompletionResult r, LocalDate weekStart) {
    return r.weeks().stream().filter(w -> w.weekStart().equals(weekStart)).findFirst().orElseThrow();
  }

  @Test
  @DisplayName("모든 조건을 채우면 eligible, 경고 없음")
  void allSatisfied() {
    CompletionResult r = CompletionCalculator.calculate(base().build());

    assertThat(r.eligible()).isTrue();
    assertThat(r.warnings()).isEmpty();
    assertThat(r.weeks()).extracting(Week::satisfied).containsExactly(true, true, true);
  }

  @Nested
  @DisplayName("필요 참석 인원")
  class Required {

    @Test
    @DisplayName("비율 0.5 에 명단 5명이면 3명 (올림)")
    void halfOfFiveIsThree() {
      assertThat(CompletionCalculator.required(5, HALF)).isEqualTo(3);
    }

    @Test
    @DisplayName("딱 나누어떨어지면 올리지 않는다 (0.5 × 4 = 2)")
    void exactIsNotRoundedUp() {
      assertThat(CompletionCalculator.required(4, HALF)).isEqualTo(2);
    }

    @Test
    @DisplayName("부동소수로 틀리기 쉬운 값도 정확하다 (0.1 × 30 = 3, 0.7 × 10 = 7)")
    void noFloatingPointError() {
      assertThat(CompletionCalculator.required(30, new BigDecimal("0.10"))).isEqualTo(3);
      assertThat(CompletionCalculator.required(10, new BigDecimal("0.70"))).isEqualTo(7);
    }

    @Test
    @DisplayName("명단 5명 중 2명 출석이면 인정되지 않는다")
    void underRequiredIsNotCounted() {
      CompletionResult r =
          CompletionCalculator.calculate(
              base()
                  .activities(
                      List.of(
                          act(W1.plusDays(1), APPROVED, 5, 2),
                          act(W2.plusDays(1), APPROVED, 5, 3),
                          act(W3.plusDays(1), APPROVED, 5, 3)))
                  .build());

      assertThat(week(r, W1).activities().get(0).required()).isEqualTo(3);
      assertThat(week(r, W1).activities().get(0).counted()).isFalse();
      assertThat(week(r, W1).satisfied()).isFalse();
      assertThat(r.eligible()).isFalse();
      assertThat(r.warnings()).containsExactly(WEEK_MISSED);
    }
  }

  @Nested
  @DisplayName("인정 활동")
  class Counted {

    @Test
    @DisplayName("한 주에 기록이 여러 개고 하나만 인정돼도 그 주는 충족")
    void oneOfManyIsEnough() {
      CompletionResult r =
          CompletionCalculator.calculate(
              base()
                  .activities(
                      List.of(
                          act(W1.plusDays(0), APPROVED, 5, 1),
                          act(W1.plusDays(2), PENDING, 5, 5),
                          act(W1.plusDays(4), APPROVED, 5, 3),
                          act(W2.plusDays(2), APPROVED, 5, 3),
                          act(W3.plusDays(2), APPROVED, 5, 3)))
                  .build());

      assertThat(week(r, W1).activities())
          .extracting(CompletionResult.WeekActivity::counted)
          .containsExactly(false, false, true);
      assertThat(week(r, W1).satisfied()).isTrue();
      assertThat(r.eligible()).isTrue();
    }

    @Test
    @DisplayName("명단이 비어 있는(0/0) 기록은 인증 완료여도 인정되지 않는다")
    void emptyRosterIsNotCounted() {
      CompletionResult r =
          CompletionCalculator.calculate(
              base()
                  .activities(
                      List.of(
                          act(W1.plusDays(1), APPROVED, 0, 0),
                          act(W2.plusDays(1), APPROVED, 5, 3),
                          act(W3.plusDays(1), APPROVED, 5, 3)))
                  .build());

      assertThat(week(r, W1).activities().get(0).required()).isZero();
      assertThat(week(r, W1).activities().get(0).counted()).isFalse();
      assertThat(week(r, W1).satisfied()).isFalse();
      assertThat(r.eligible()).isFalse();
    }

    @Test
    @DisplayName("인증 완료가 아닌 기록은 인원을 채워도 인정되지 않는다")
    void onlyApprovedCounts() {
      CompletionResult r =
          CompletionCalculator.calculate(
              base()
                  .activities(
                      List.of(
                          act(W1.plusDays(1), PENDING, 5, 5),
                          act(W2.plusDays(1), REVISION_REQUESTED, 5, 5),
                          act(W3.plusDays(1), APPROVED, 5, 3)))
                  .build());

      assertThat(r.weeks()).extracting(Week::satisfied).containsExactly(false, false, true);
      assertThat(r.pendingActivityCount()).isEqualTo(1);
      assertThat(r.eligible()).isFalse();
    }
  }

  @Nested
  @DisplayName("주차 경계 (월요일 시작, 활동일 기준)")
  class WeekBoundary {

    @Test
    @DisplayName("일요일 활동은 그 주에 든다")
    void sundayBelongsToSameWeek() {
      LocalDate sunday = W1.plusDays(6);
      CompletionResult r =
          CompletionCalculator.calculate(
              base().activities(List.of(act(sunday, APPROVED, 4, 2))).build());

      assertThat(week(r, W1).satisfied()).isTrue();
      assertThat(week(r, W2).satisfied()).isFalse();
    }

    @Test
    @DisplayName("월요일 활동은 다음 주에 든다")
    void mondayStartsNextWeek() {
      CompletionResult r =
          CompletionCalculator.calculate(
              base().activities(List.of(act(W2, APPROVED, 4, 2))).build());

      assertThat(week(r, W1).satisfied()).isFalse();
      assertThat(week(r, W2).satisfied()).isTrue();
    }

    @Test
    @DisplayName("기간 시작·끝이 주 중간이면 그 주도 포함한다")
    void partialWeeksAreIncluded() {
      LocalDate wednesday = W1.plusDays(2);
      LocalDate nextTuesday = W2.plusDays(1);
      CompletionResult r =
          CompletionCalculator.calculate(
              base()
                  .period(wednesday, nextTuesday)
                  .activities(
                      List.of(act(W1.plusDays(0), APPROVED, 4, 2), act(W2.plusDays(5), APPROVED, 4, 2)))
                  .build());

      assertThat(r.weeks()).extracting(Week::weekStart).containsExactly(W1, W2);
      assertThat(r.outOfPeriodActivityCount()).isZero();
      assertThat(r.eligible()).isTrue();
    }

    @Test
    @DisplayName("기간 밖 주의 기록은 계산에서 빼고 경고한다")
    void outOfPeriodIsExcludedAndWarned() {
      List<Activity> acts = new java.util.ArrayList<>(fullWeeks());
      acts.add(act(W1.minusDays(1), APPROVED, 5, 5));
      acts.add(act(AFTER_END, APPROVED, 5, 5));
      CompletionResult r = CompletionCalculator.calculate(base().activities(acts).build());

      assertThat(r.outOfPeriodActivityCount()).isEqualTo(2);
      assertThat(r.weeks()).hasSize(3);
      assertThat(r.warnings()).containsExactly(OUT_OF_PERIOD);
      assertThat(r.eligible()).isTrue();
    }
  }

  @Nested
  @DisplayName("쉬는 주")
  class RestWeek {

    @Test
    @DisplayName("쉬는 주는 기록이 없어도 판정을 막지 않는다")
    void restWeekNeedsNoActivity() {
      CompletionResult r =
          CompletionCalculator.calculate(
              base()
                  .rest(W2)
                  .activities(
                      List.of(act(W1.plusDays(1), APPROVED, 5, 3), act(W3.plusDays(1), APPROVED, 5, 3)))
                  .build());

      assertThat(week(r, W2).rest()).isTrue();
      assertThat(week(r, W2).satisfied()).isNull();
      assertThat(r.warnings()).isEmpty();
      assertThat(r.eligible()).isTrue();
    }
  }

  @Nested
  @DisplayName("진행 중")
  class InProgress {

    @Test
    @DisplayName("이번 주와 아직 오지 않은 주는 기록이 없으면 null, 놓친 주로 보지 않는다")
    void currentAndFutureWeeksArePending() {
      CompletionResult r =
          CompletionCalculator.calculate(
              base()
                  .today(W2.plusDays(3))
                  .activities(List.of(act(W1.plusDays(1), APPROVED, 5, 3)))
                  .build());

      assertThat(r.weeks()).extracting(Week::satisfied).containsExactly(true, null, null);
      assertThat(r.warnings()).doesNotContain(WEEK_MISSED);
      assertThat(r.eligible()).isFalse();
    }

    @Test
    @DisplayName("이번 주라도 인정 활동이 있으면 바로 충족")
    void currentWeekWithActivityIsSatisfied() {
      CompletionResult r =
          CompletionCalculator.calculate(
              base()
                  .today(W2.plusDays(3))
                  .activities(
                      List.of(act(W1.plusDays(1), APPROVED, 5, 3), act(W2.plusDays(1), APPROVED, 5, 3)))
                  .build());

      assertThat(r.weeks()).extracting(Week::satisfied).containsExactly(true, true, null);
    }
  }

  @Nested
  @DisplayName("팀원·목표·정원")
  class MembersAndGoal {

    @Test
    @DisplayName("팀원 3명이면 불충족")
    void threeMembersIsNotEnough() {
      CompletionResult r = CompletionCalculator.calculate(base().members(3).build());

      assertThat(r.memberCountSatisfied()).isFalse();
      assertThat(r.eligible()).isFalse();
      assertThat(r.warnings()).containsExactly(MEMBERS_UNDER_4);
    }

    @Test
    @DisplayName("목표가 ACHIEVED 가 아니면 불충족")
    void goalMustBeAchieved() {
      for (ClubGoalStatus status :
          List.of(
              ClubGoalStatus.NOT_SUBMITTED, ClubGoalStatus.SUBMITTED, ClubGoalStatus.NOT_ACHIEVED)) {
        assertThat(CompletionCalculator.calculate(base().goal(status).build()).eligible())
            .as(status.name())
            .isFalse();
      }
    }

    @Test
    @DisplayName("목표 미등록·정원 초과는 경고만 한다")
    void goalNotSetAndOverCapacityAreWarnings() {
      CompletionResult r =
          CompletionCalculator.calculate(base().goalSet(false).members(6).capacity(5).build());

      assertThat(r.warnings()).containsExactly(GOAL_NOT_SET, OVER_CAPACITY);
    }
  }

  @Nested
  @DisplayName("기간 미설정")
  class PeriodNotSet {

    @Test
    @DisplayName("기간이 없으면 eligible=false 와 PERIOD_NOT_SET, 주차 계산은 생략")
    void periodNotSet() {
      CompletionResult r = CompletionCalculator.calculate(base().period(null, null).build());

      assertThat(r.period()).isNull();
      assertThat(r.weeks()).isEmpty();
      assertThat(r.eligible()).isFalse();
      assertThat(r.warnings()).containsExactly(PERIOD_NOT_SET);
    }

    @Test
    @DisplayName("끝 날짜만 비어도 기간 미설정")
    void endDateMissing() {
      CompletionResult r = CompletionCalculator.calculate(base().period(W1, null).build());

      assertThat(r.warnings()).contains(PERIOD_NOT_SET);
      assertThat(r.eligible()).isFalse();
    }

    @Test
    @DisplayName("기간이 뒤집혀 주가 하나도 없으면 eligible 이 아니다")
    void invertedPeriodIsNotEligible() {
      CompletionResult r = CompletionCalculator.calculate(base().period(END, W1).build());

      assertThat(r.weeks()).isEmpty();
      assertThat(r.eligible()).isFalse();
    }
  }

  @Test
  @DisplayName("쉬는 주 저장용 월요일 맞춤: 어떤 요일이든 그 주 월요일")
  void weekStartOf() {
    assertThat(CompletionCalculator.weekStartOf(W1)).isEqualTo(W1);
    assertThat(CompletionCalculator.weekStartOf(W1.plusDays(6))).isEqualTo(W1);
    assertThat(CompletionCalculator.weekStartOf(W2)).isEqualTo(W2);
  }
}
