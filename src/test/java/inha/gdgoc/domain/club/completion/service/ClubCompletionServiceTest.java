package inha.gdgoc.domain.club.completion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import inha.gdgoc.domain.club.activity.dto.ApprovedActivity;
import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import inha.gdgoc.domain.club.activity.service.ClubActivityQueryService;
import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.entity.ClubTerm;
import inha.gdgoc.domain.club.club.service.ClubAccessService;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.domain.club.completion.dto.request.ClubCompletionDecisionRequest;
import inha.gdgoc.domain.club.completion.dto.request.ClubGoalRequest;
import inha.gdgoc.domain.club.completion.dto.request.ClubGoalResultRequest;
import inha.gdgoc.domain.club.completion.dto.response.ClubCompletionResponse;
import inha.gdgoc.domain.club.completion.entity.ClubCompletion;
import inha.gdgoc.domain.club.completion.entity.ClubRestWeek;
import inha.gdgoc.domain.club.completion.enums.ClubCompletionStatus;
import inha.gdgoc.domain.club.completion.enums.ClubCompletionWarning;
import inha.gdgoc.domain.club.completion.enums.ClubGoalStatus;
import inha.gdgoc.domain.club.completion.exception.ClubCompletionErrorCode;
import inha.gdgoc.domain.club.completion.repository.ClubCompletionRepository;
import inha.gdgoc.domain.club.completion.repository.ClubRestWeekRepository;
import inha.gdgoc.domain.club.member.service.ClubMemberQueryService;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.security.AccessGuard;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

/** 목표·쉬는 주·완주 서비스. 권한 경계(리더·운영진)와 「막지 않는다」 원칙을 고정한다. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClubCompletionServiceTest {

  private static final Long CLUB_ID = 1L;
  private static final Long LEADER_ID = 10L;
  private static final Long OTHER_ID = 20L;
  private static final Long STAFF_ID = 30L;
  private static final LocalDate MON = LocalDate.of(2026, 10, 5);

  @Mock private ClubAccessService clubAccessService;
  @Mock private ClubMemberQueryService clubMemberQueryService;
  @Mock private ClubActivityQueryService clubActivityQueryService;
  @Mock private ClubCompletionRepository clubCompletionRepository;
  @Mock private ClubRestWeekRepository clubRestWeekRepository;

  private ClubCompletionService service;
  private Club club;

  @BeforeEach
  void setUp() {
    service =
        new ClubCompletionService(
            clubAccessService,
            clubMemberQueryService,
            clubActivityQueryService,
            clubCompletionRepository,
            clubRestWeekRepository,
            new AccessGuard());
    ClubTerm term = ClubTerm.create("2026-2", new BigDecimal("0.50"));
    club = BeanUtils.instantiateClass(Club.class);
    ReflectionTestUtils.setField(club, "id", CLUB_ID);
    ReflectionTestUtils.setField(club, "term", term);
    given(clubAccessService.getClub(CLUB_ID)).willReturn(club);
    given(clubAccessService.isLeader(CLUB_ID, LEADER_ID)).willReturn(true);
    given(clubAccessService.requireLeader(CLUB_ID, LEADER_ID)).willReturn(club);
    given(clubAccessService.requireLeader(CLUB_ID, OTHER_ID))
        .willThrow(new BusinessException(ClubErrorCode.NOT_CLUB_LEADER));
    given(clubCompletionRepository.findByClubId(CLUB_ID)).willReturn(Optional.empty());
    given(clubCompletionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
    given(clubRestWeekRepository.findAllByClubIdOrderByWeekStart(CLUB_ID)).willReturn(List.of());
  }

  private static CustomUserDetails user(Long id, UserRole role) {
    return new CustomUserDetails(id, "u" + id, "s", List.of(), role, TeamType.HR);
  }

  @Nested
  @DisplayName("쉬는 주")
  class RestWeeks {

    @Test
    @DisplayName("리더는 지정할 수 있고, 아무 요일이나 그 주 월요일로 맞춘다")
    void leaderCanSetAndDatesAreNormalized() {
      List<LocalDate> saved =
          service.replaceRestWeeks(
              CLUB_ID,
              user(LEADER_ID, UserRole.MEMBER),
              List.of(MON.plusDays(3), MON.plusDays(6), MON.plusWeeks(2)));

      assertThat(saved).containsExactly(MON, MON.plusWeeks(2));
    }

    @Test
    @DisplayName("운영진은 리더가 아니어도 지정할 수 있다")
    void staffCanSet() {
      List<LocalDate> saved =
          service.replaceRestWeeks(CLUB_ID, user(STAFF_ID, UserRole.CORE), List.of(MON));

      assertThat(saved).containsExactly(MON);
    }

    @Test
    @DisplayName("리더도 운영진도 아닌 부원은 403")
    void otherMemberIsForbidden() {
      assertThatThrownBy(
              () ->
                  service.replaceRestWeeks(
                      CLUB_ID, user(OTHER_ID, UserRole.MEMBER), List.of(MON)))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ClubCompletionErrorCode.NOT_LEADER_OR_STAFF);
      verify(clubRestWeekRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("이미 있는 주는 그대로 두고, 빠진 주만 지우고 새 주만 넣는다")
    void appliesOnlyTheDifference() {
      ClubRestWeek kept = ClubRestWeek.create(club, MON);
      ClubRestWeek dropped = ClubRestWeek.create(club, MON.plusWeeks(1));
      given(clubRestWeekRepository.findAllByClubIdOrderByWeekStart(CLUB_ID))
          .willReturn(List.of(kept, dropped));

      service.replaceRestWeeks(
          CLUB_ID, user(LEADER_ID, UserRole.MEMBER), List.of(MON, MON.plusWeeks(3)));

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<ClubRestWeek>> deleted = ArgumentCaptor.forClass(List.class);
      verify(clubRestWeekRepository).deleteAll(deleted.capture());
      assertThat(deleted.getValue()).containsExactly(dropped);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<ClubRestWeek>> added = ArgumentCaptor.forClass(List.class);
      verify(clubRestWeekRepository).saveAll(added.capture());
      assertThat(added.getValue())
          .extracting(ClubRestWeek::getWeekStart)
          .containsExactly(MON.plusWeeks(3));
    }
  }

  @Nested
  @DisplayName("목표")
  class Goal {

    @Test
    @DisplayName("리더가 아니면 목표를 등록할 수 없다")
    void onlyLeaderSetsGoal() {
      assertThatThrownBy(
              () -> service.updateGoal(CLUB_ID, OTHER_ID, new ClubGoalRequest("목표", null)))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ClubErrorCode.NOT_CLUB_LEADER);
    }

    @Test
    @DisplayName("목표 행은 처음 쓸 때 만든다")
    void createsRowOnFirstWrite() {
      service.updateGoal(CLUB_ID, LEADER_ID, new ClubGoalRequest("  알고리즘 50문제  ", " "));

      ArgumentCaptor<ClubCompletion> saved = ArgumentCaptor.forClass(ClubCompletion.class);
      verify(clubCompletionRepository).save(saved.capture());
      assertThat(saved.getValue().getGoal()).isEqualTo("알고리즘 50문제");
      assertThat(saved.getValue().getGoalCriteria()).isNull();
      assertThat(saved.getValue().getGoalStatus()).isEqualTo(ClubGoalStatus.NOT_SUBMITTED);
    }

    @Test
    @DisplayName("판정 뒤에 결과를 다시 내면 SUBMITTED 로 돌아간다")
    void resubmitResetsToSubmitted() {
      ClubCompletion completion = ClubCompletion.create(club);
      completion.judgeGoal(ClubGoalStatus.ACHIEVED);
      given(clubCompletionRepository.findByClubId(CLUB_ID)).willReturn(Optional.of(completion));

      service.submitGoalResult(
          CLUB_ID, LEADER_ID, new ClubGoalResultRequest("50문제 풂", List.of("https://x/1.png")));

      assertThat(completion.getGoalStatus()).isEqualTo(ClubGoalStatus.SUBMITTED);
      assertThat(completion.getGoalEvidenceUrls()).containsExactly("https://x/1.png");
    }

    @Test
    @DisplayName("운영진 판정은 ACHIEVED·NOT_ACHIEVED 만 받는다")
    void judgeAcceptsOnlyFinalStatuses() {
      service.judgeGoal(CLUB_ID, ClubGoalStatus.ACHIEVED);

      assertThatThrownBy(() -> service.judgeGoal(CLUB_ID, ClubGoalStatus.SUBMITTED))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ClubCompletionErrorCode.GOAL_JUDGEMENT_INVALID);
    }
  }

  @Nested
  @DisplayName("완주")
  class Completion {

    @Test
    @DisplayName("행이 없으면 기본값으로 응답하고, 기간 미설정 경고를 띄운다")
    void defaultsWhenNoRow() {
      ClubCompletionResponse r = service.getCompletion(CLUB_ID);

      assertThat(r.goalStatus()).isEqualTo(ClubGoalStatus.NOT_SUBMITTED);
      assertThat(r.completionStatus()).isEqualTo(ClubCompletionStatus.IN_PROGRESS);
      assertThat(r.goal()).isNull();
      assertThat(r.goalEvidenceUrls()).isEmpty();
      assertThat(r.period()).isNull();
      assertThat(r.warnings()).contains(ClubCompletionWarning.PERIOD_NOT_SET);
      assertThat(r.eligible()).isFalse();
    }

    @Test
    @DisplayName("검토 대기 기록도 주차 표에 들어가고 대기 수로 센다")
    void pendingActivitiesAreShown() {
      ReflectionTestUtils.setField(club, "startDate", MON);
      ReflectionTestUtils.setField(club, "endDate", MON.plusDays(6));
      given(
              clubActivityQueryService.findByStatus(
                  eq(CLUB_ID), eq(ClubActivityStatus.PENDING), any(), any()))
          .willReturn(List.of(new ApprovedActivity(7L, MON.plusDays(1), 4, 4)));

      ClubCompletionResponse r = service.getCompletion(CLUB_ID);

      assertThat(r.pendingActivityCount()).isEqualTo(1);
      assertThat(r.weeks()).hasSize(1);
      assertThat(r.weeks().get(0).activities().get(0).status())
          .isEqualTo(ClubActivityStatus.PENDING);
      assertThat(r.weeks().get(0).activities().get(0).counted()).isFalse();
    }

    @Test
    @DisplayName("계산값과 무관하게 COMPLETED 로 확정할 수 있다")
    void confirmIgnoresCalculation() {
      service.confirm(
          CLUB_ID, STAFF_ID, new ClubCompletionDecisionRequest(ClubCompletionStatus.COMPLETED, "사유"));

      ArgumentCaptor<ClubCompletion> saved = ArgumentCaptor.forClass(ClubCompletion.class);
      verify(clubCompletionRepository).save(saved.capture());
      assertThat(saved.getValue().getCompletionStatus()).isEqualTo(ClubCompletionStatus.COMPLETED);
      assertThat(saved.getValue().getCompletionMemo()).isEqualTo("사유");
      assertThat(saved.getValue().getConfirmedBy()).isEqualTo(STAFF_ID);
      assertThat(saved.getValue().getConfirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("IN_PROGRESS 로는 확정할 수 없다")
    void confirmRejectsInProgress() {
      assertThatThrownBy(
              () ->
                  service.confirm(
                      CLUB_ID,
                      STAFF_ID,
                      new ClubCompletionDecisionRequest(ClubCompletionStatus.IN_PROGRESS, null)))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ClubCompletionErrorCode.COMPLETION_DECISION_INVALID);
    }
  }
}
