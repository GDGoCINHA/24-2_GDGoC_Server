package inha.gdgoc.domain.club.completion.service;

import inha.gdgoc.domain.club.activity.dto.ApprovedActivity;
import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import inha.gdgoc.domain.club.activity.service.ClubActivityQueryService;
import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.service.ClubAccessService;
import inha.gdgoc.domain.club.completion.dto.CompletionInput;
import inha.gdgoc.domain.club.completion.dto.CompletionInput.Activity;
import inha.gdgoc.domain.club.completion.dto.CompletionResult;
import inha.gdgoc.domain.club.completion.dto.request.ClubCompletionDecisionRequest;
import inha.gdgoc.domain.club.completion.dto.request.ClubGoalRequest;
import inha.gdgoc.domain.club.completion.dto.request.ClubGoalResultRequest;
import inha.gdgoc.domain.club.completion.dto.response.ClubCompletionResponse;
import inha.gdgoc.domain.club.completion.entity.ClubCompletion;
import inha.gdgoc.domain.club.completion.entity.ClubRestWeek;
import inha.gdgoc.domain.club.completion.enums.ClubCompletionStatus;
import inha.gdgoc.domain.club.completion.enums.ClubGoalStatus;
import inha.gdgoc.domain.club.completion.exception.ClubCompletionErrorCode;
import inha.gdgoc.domain.club.completion.repository.ClubCompletionRepository;
import inha.gdgoc.domain.club.completion.repository.ClubRestWeekRepository;
import inha.gdgoc.domain.club.member.service.ClubMemberQueryService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.security.AccessGuard;
import inha.gdgoc.global.security.AccessGuard.AccessCondition;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 목표·쉬는 주·완주. 계산은 {@link CompletionCalculator} 가 하고, 여기서는 입력을 모으고 권한을 본다.
 *
 * <p>막지 말고 보여준다 — 목표 판정·완주 확정은 순서나 계산값과 무관하게 받는다. 막는 것은 권한과 허용되지 않는 상태값뿐이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubCompletionService {

  static final ZoneId KST = ZoneId.of("Asia/Seoul");

  /** 상태별 활동 조회의 기간. 기간 밖 기록도 세야 하므로 사실상 전체다. */
  private static final LocalDate ALL_FROM = LocalDate.of(2000, 1, 1);

  private static final LocalDate ALL_TO = LocalDate.of(2999, 12, 31);

  /** 주차 표에 보여줄 상태. 인정은 APPROVED 만이지만 검토 대기·보완 요청도 표에 보여야 이끔이가 이유를 안다. */
  private static final List<ClubActivityStatus> SHOWN_STATUSES =
      List.of(
          ClubActivityStatus.APPROVED,
          ClubActivityStatus.PENDING,
          ClubActivityStatus.REVISION_REQUESTED);

  private final ClubAccessService clubAccessService;
  private final ClubMemberQueryService clubMemberQueryService;
  private final ClubActivityQueryService clubActivityQueryService;
  private final ClubCompletionRepository clubCompletionRepository;
  private final ClubRestWeekRepository clubRestWeekRepository;
  private final AccessGuard accessGuard;

  // --- 조회 ---------------------------------------------------------------

  public ClubCompletionResponse getCompletion(Long clubId) {
    return getCompletion(clubAccessService.getClub(clubId));
  }

  /** 운영진 현황 표도 같은 계산을 쓴다 — 팀 화면과 현황 표의 숫자가 어긋나면 안 된다. */
  public ClubCompletionResponse getCompletion(Club club) {
    ClubCompletion completion = clubCompletionRepository.findByClubId(club.getId()).orElse(null);
    List<LocalDate> restWeeks = restWeeksOf(club.getId());
    return ClubCompletionResponse.of(calculate(club, completion, restWeeks), restWeeks, completion);
  }

  private CompletionResult calculate(
      Club club, ClubCompletion completion, List<LocalDate> restWeeks) {
    List<Activity> activities = new ArrayList<>();
    for (ClubActivityStatus status : SHOWN_STATUSES) {
      for (ApprovedActivity a :
          clubActivityQueryService.findByStatus(club.getId(), status, ALL_FROM, ALL_TO)) {
        activities.add(
            new Activity(
                a.activityId(),
                a.activityDate(),
                status,
                Math.toIntExact(a.rosterCount()),
                Math.toIntExact(a.attendedCount())));
      }
    }
    CompletionInput input =
        new CompletionInput(
            club.getStartDate(),
            club.getEndDate(),
            Set.copyOf(restWeeks),
            club.getTerm().getAttendanceRatio(),
            activities,
            Math.toIntExact(clubMemberQueryService.countActive(club.getId())),
            club.getCapacity(),
            completion != null && completion.hasGoal(),
            completion != null ? completion.getGoalStatus() : ClubGoalStatus.NOT_SUBMITTED,
            LocalDate.now(KST));
    return CompletionCalculator.calculate(input);
  }

  private List<LocalDate> restWeeksOf(Long clubId) {
    return clubRestWeekRepository.findAllByClubIdOrderByWeekStart(clubId).stream()
        .map(ClubRestWeek::getWeekStart)
        .toList();
  }

  // --- 리더 ----------------------------------------------------------------

  @Transactional
  public void updateGoal(Long clubId, Long userId, ClubGoalRequest req) {
    Club club = clubAccessService.requireLeader(clubId, userId);
    getOrCreate(club).updateGoal(req.goal().strip(), strip(req.goalCriteria()));
  }

  @Transactional
  public void submitGoalResult(Long clubId, Long userId, ClubGoalResultRequest req) {
    Club club = clubAccessService.requireLeader(clubId, userId);
    getOrCreate(club).submitResult(req.goalResult().strip(), req.evidenceUrls());
  }

  /**
   * 쉬는 주를 통째로 바꾼다. 리더 또는 운영진. 받은 날짜는 그 주 월요일로 맞추고 중복은 하나로 친다.
   *
   * <p>지우고 다시 넣지 않고 차이만 반영한다 — 같은 주를 지웠다 넣으면 flush 순서에 따라 유니크 제약에 걸린다.
   */
  @Transactional
  public List<LocalDate> replaceRestWeeks(Long clubId, CustomUserDetails me, List<LocalDate> weeks) {
    Club club = requireLeaderOrStaff(clubId, me);
    Set<LocalDate> wanted =
        weeks.stream().map(CompletionCalculator::weekStartOf).collect(Collectors.toSet());

    Map<LocalDate, ClubRestWeek> existing =
        clubRestWeekRepository.findAllByClubIdOrderByWeekStart(clubId).stream()
            .collect(Collectors.toMap(ClubRestWeek::getWeekStart, Function.identity()));

    List<ClubRestWeek> removed =
        existing.values().stream().filter(w -> !wanted.contains(w.getWeekStart())).toList();
    clubRestWeekRepository.deleteAll(removed);

    List<ClubRestWeek> added =
        wanted.stream()
            .filter(w -> !existing.containsKey(w))
            .map(w -> ClubRestWeek.create(club, w))
            .toList();
    clubRestWeekRepository.saveAll(added);

    return List.copyOf(new TreeSet<>(wanted));
  }

  // --- 운영진 ---------------------------------------------------------------

  @Transactional
  public void judgeGoal(Long clubId, ClubGoalStatus status) {
    if (status != ClubGoalStatus.ACHIEVED && status != ClubGoalStatus.NOT_ACHIEVED) {
      throw new BusinessException(ClubCompletionErrorCode.GOAL_JUDGEMENT_INVALID);
    }
    getOrCreate(clubAccessService.getClub(clubId)).judgeGoal(status);
  }

  @Transactional
  public void confirm(Long clubId, Long confirmerId, ClubCompletionDecisionRequest req) {
    if (req.status() != ClubCompletionStatus.COMPLETED
        && req.status() != ClubCompletionStatus.FAILED) {
      throw new BusinessException(ClubCompletionErrorCode.COMPLETION_DECISION_INVALID);
    }
    getOrCreate(clubAccessService.getClub(clubId))
        .confirm(req.status(), strip(req.memo()), confirmerId, Instant.now());
  }

  // -------------------------------------------------------------------------

  /** 리더 또는 운영진(CORE 이상). 소모임이 없으면 404 가 먼저 난다. */
  Club requireLeaderOrStaff(Long clubId, CustomUserDetails me) {
    Club club = clubAccessService.getClub(clubId);
    boolean staff = accessGuard.check(me, AccessCondition.atLeast(UserRole.CORE));
    boolean leader = me != null && clubAccessService.isLeader(clubId, me.getUserId());
    if (!staff && !leader) {
      throw new BusinessException(ClubCompletionErrorCode.NOT_LEADER_OR_STAFF);
    }
    return club;
  }

  private ClubCompletion getOrCreate(Club club) {
    return clubCompletionRepository
        .findByClubId(club.getId())
        .orElseGet(() -> clubCompletionRepository.save(ClubCompletion.create(club)));
  }

  private static String strip(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }
}
