package inha.gdgoc.domain.club.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import inha.gdgoc.domain.club.activity.dto.response.ClubFixRequestResponse;
import inha.gdgoc.domain.club.activity.dto.response.MyAttendanceResponse;
import inha.gdgoc.domain.club.activity.entity.ClubActivity;
import inha.gdgoc.domain.club.activity.entity.ClubActivityAttendance;
import inha.gdgoc.domain.club.activity.entity.ClubAttendanceFixRequest;
import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import inha.gdgoc.domain.club.activity.enums.ClubFixRequestStatus;
import inha.gdgoc.domain.club.activity.exception.ClubActivityErrorCode;
import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.entity.ClubTerm;
import inha.gdgoc.domain.club.club.enums.ClubCategory;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.domain.club.member.entity.ClubMember;
import inha.gdgoc.domain.club.schedule.entity.ClubSchedule;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * 출석 수정 요청과 내 출석을 실제 DB 로 검증한다.
 *
 * <p>1월 7일 기록의 명단은 {리더(출석), 민지(결석), 도윤(출석)}. 외부인은 명단에 없다. 날짜는 수락 시각(지금)보다 앞서도록 과거로 둔다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ClubAttendanceServiceTest {

  private static final LocalDate DAY = LocalDate.of(2026, 1, 7);
  private static final Instant JOINED = Instant.parse("2025-12-01T00:00:00Z");
  private static final Instant SUBMITTED = Instant.parse("2026-01-07T12:00:00Z");
  private static final Long REVIEWER_ID = 99L;

  @Autowired private ClubAttendanceService clubAttendanceService;
  @Autowired private EntityManager entityManager;

  private int userSeq;
  private Club club;
  private User leader;
  private User minji;
  private User doyun;
  private User outsider;
  private ClubActivity activity;

  @BeforeEach
  void setUp() {
    leader = user("리더");
    minji = user("민지");
    doyun = user("도윤");
    outsider = user("외부인");
    club = club(leader);
    persist(ClubMember.founder(club, leader, JOINED));
    join(minji);
    join(doyun);

    activity = activity(DAY, null);
    roster(activity, leader, true);
    roster(activity, minji, false);
    roster(activity, doyun, true);
    flushAndClear();
  }

  // ---- 요청 ----

  @Test
  @DisplayName("명단에 있는 사람은 자기 출석의 수정을 요청한다. 요청 값은 지금과 반대다")
  void memberRequestsFix() {
    Long id = clubAttendanceService.requestFix(activity.getId(), minji.getId(), " 늦게 왔어요 ");
    flushAndClear();

    ClubAttendanceFixRequest saved = entityManager.find(ClubAttendanceFixRequest.class, id);
    assertThat(saved.isPending()).isTrue();
    assertThat(saved.isRequestedAttended()).isTrue();
    assertThat(saved.getReason()).isEqualTo("늦게 왔어요");
  }

  @Test
  @DisplayName("명단에 없으면 요청할 수 없다")
  void outsiderCannotRequest() {
    assertError(
        () -> clubAttendanceService.requestFix(activity.getId(), outsider.getId(), "저도요"),
        ClubActivityErrorCode.NOT_IN_ROSTER);
  }

  @Test
  @DisplayName("인증 완료된 기록에는 요청할 수 없다")
  void approvedActivityRejectsRequest() {
    approve(activity);

    assertError(
        () -> clubAttendanceService.requestFix(activity.getId(), minji.getId(), "늦게 왔어요"),
        ClubActivityErrorCode.ACTIVITY_LOCKED);
  }

  @Test
  @DisplayName("대기 중인 요청이 있으면 다시 낼 수 없고, 처리된 뒤에는 다시 낼 수 있다")
  void onePendingRequestAtATime() {
    Long first = clubAttendanceService.requestFix(activity.getId(), minji.getId(), "1");
    flushAndClear();

    assertError(
        () -> clubAttendanceService.requestFix(activity.getId(), minji.getId(), "2"),
        ClubActivityErrorCode.FIX_REQUEST_ALREADY_PENDING);

    clubAttendanceService.reject(first, leader.getId());
    flushAndClear();
    assertThat(clubAttendanceService.requestFix(activity.getId(), minji.getId(), "3")).isNotNull();
  }

  // ---- 수락 ----

  @Test
  @DisplayName("수락하면 출석이 요청한 값이 되고, 기록은 확인 중으로 돌아가 다시 검토받는다")
  void acceptAppliesAndReopens() {
    requestRevision(activity);
    Long id = clubAttendanceService.requestFix(activity.getId(), minji.getId(), "늦게 왔어요");
    flushAndClear();

    clubAttendanceService.accept(id, leader.getId());
    flushAndClear();

    assertThat(attended(minji)).isTrue();
    ClubActivity reopened = entityManager.find(ClubActivity.class, activity.getId());
    assertThat(reopened.getStatus()).isEqualTo(ClubActivityStatus.PENDING);
    assertThat(reopened.getSubmittedAt()).isAfter(SUBMITTED);
    assertThat(entityManager.find(ClubAttendanceFixRequest.class, id).getStatus())
        .isEqualTo(ClubFixRequestStatus.ACCEPTED);
  }

  @Test
  @DisplayName("요청 뒤 리더가 직접 고쳤으면 수락해도 다시 뒤집히지 않는다")
  void acceptDoesNotFlipLeadersFix() {
    Long id = clubAttendanceService.requestFix(activity.getId(), minji.getId(), "늦게 왔어요");
    attendanceRow(minji).mark(true); // 리더가 활동 기록을 고쳐 출석으로 바꿨다
    flushAndClear();

    clubAttendanceService.accept(id, leader.getId());
    flushAndClear();

    assertThat(attended(minji)).isTrue();
  }

  @Test
  @DisplayName("기록이 그 사이 인증 완료됐으면 수락은 409, 거절은 된다")
  void approvedActivityBlocksAcceptButNotReject() {
    Long id = clubAttendanceService.requestFix(activity.getId(), minji.getId(), "늦게 왔어요");
    approve(activity);

    assertError(
        () -> clubAttendanceService.accept(id, leader.getId()),
        ClubActivityErrorCode.ACTIVITY_LOCKED);
    assertThat(attended(minji)).isFalse();

    clubAttendanceService.reject(id, leader.getId());
    flushAndClear();
    assertThat(entityManager.find(ClubAttendanceFixRequest.class, id).getStatus())
        .isEqualTo(ClubFixRequestStatus.REJECTED);
  }

  @Test
  @DisplayName("요청자가 명단에서 빠졌으면(활동일 변경) 수락할 수 없다")
  void requesterNoLongerInRoster() {
    Long id = clubAttendanceService.requestFix(activity.getId(), minji.getId(), "늦게 왔어요");
    entityManager.remove(attendanceRow(minji));
    flushAndClear();

    assertError(
        () -> clubAttendanceService.accept(id, leader.getId()),
        ClubActivityErrorCode.FIX_REQUEST_NOT_IN_ROSTER);
  }

  @Test
  @DisplayName("리더가 아니면 수락·거절·목록을 볼 수 없다")
  void onlyLeaderHandles() {
    Long id = clubAttendanceService.requestFix(activity.getId(), minji.getId(), "늦게 왔어요");
    flushAndClear();

    assertError(
        () -> clubAttendanceService.accept(id, doyun.getId()), ClubErrorCode.NOT_CLUB_LEADER);
    assertError(
        () -> clubAttendanceService.reject(id, doyun.getId()), ClubErrorCode.NOT_CLUB_LEADER);
    assertError(
        () ->
            clubAttendanceService.listForLeader(
                club.getId(), doyun.getId(), ClubFixRequestStatus.PENDING),
        ClubErrorCode.NOT_CLUB_LEADER);
  }

  @Test
  @DisplayName("이미 처리한 요청은 다시 처리할 수 없다")
  void handledOnce() {
    Long id = clubAttendanceService.requestFix(activity.getId(), minji.getId(), "늦게 왔어요");
    clubAttendanceService.reject(id, leader.getId());
    flushAndClear();

    assertError(
        () -> clubAttendanceService.accept(id, leader.getId()),
        ClubActivityErrorCode.FIX_REQUEST_ALREADY_HANDLED);
  }

  // ---- 조회 ----

  @Test
  @DisplayName("리더의 목록에는 지금 값과 요청 값이 함께 나온다")
  void leaderListShowsCurrentAndRequested() {
    clubAttendanceService.requestFix(activity.getId(), minji.getId(), "늦게 왔어요");
    flushAndClear();

    List<ClubFixRequestResponse> list =
        clubAttendanceService.listForLeader(
            club.getId(), leader.getId(), ClubFixRequestStatus.PENDING);

    assertThat(list).hasSize(1);
    assertThat(list.get(0).userName()).isEqualTo("민지");
    assertThat(list.get(0).currentAttended()).isFalse();
    assertThat(list.get(0).requestedAttended()).isTrue();
    assertThat(list.get(0).activityDate()).isEqualTo(DAY);
  }

  @Test
  @DisplayName("내 출석은 최근 회차부터, 일정 제목과 요청 중 여부를 함께 준다")
  void myAttendanceNewestFirst() {
    ClubSchedule schedule =
        persist(ClubSchedule.create(club, "2주차", SUBMITTED, null, null, null, null));
    ClubActivity later = activity(DAY.plusDays(7), schedule);
    roster(later, minji, true);
    flushAndClear();
    clubAttendanceService.requestFix(activity.getId(), minji.getId(), "늦게 왔어요");
    flushAndClear();

    List<MyAttendanceResponse> mine =
        clubAttendanceService.myAttendance(club.getId(), minji.getId());

    assertThat(mine)
        .extracting(
            MyAttendanceResponse::activityDate,
            MyAttendanceResponse::scheduleTitle,
            MyAttendanceResponse::attended,
            MyAttendanceResponse::fixRequestPending)
        .containsExactly(
            tuple(DAY.plusDays(7), "2주차", true, false), tuple(DAY, null, false, true));
  }

  @Test
  @DisplayName("명단에 든 적 없으면 내 출석은 비어 있다")
  void myAttendanceEmptyForOutsider() {
    assertThat(clubAttendanceService.myAttendance(club.getId(), outsider.getId())).isEmpty();
  }

  // ---- 도우미 ----

  private boolean attended(User user) {
    flushAndClear();
    return attendanceRow(user).isAttended();
  }

  private ClubActivityAttendance attendanceRow(User user) {
    return entityManager
        .createQuery(
            "select a from ClubActivityAttendance a where a.activity.id = :a and a.user.id = :u",
            ClubActivityAttendance.class)
        .setParameter("a", activity.getId())
        .setParameter("u", user.getId())
        .getSingleResult();
  }

  private void approve(ClubActivity target) {
    entityManager.find(ClubActivity.class, target.getId()).approve(REVIEWER_ID, SUBMITTED);
    flushAndClear();
  }

  private void requestRevision(ClubActivity target) {
    entityManager
        .find(ClubActivity.class, target.getId())
        .requestRevision(REVIEWER_ID, "사진이 흐려요", SUBMITTED);
    flushAndClear();
  }

  private ClubActivity activity(LocalDate date, ClubSchedule schedule) {
    return persist(
        ClubActivity.create(club, schedule, date, "모임", null, leader.getId(), SUBMITTED));
  }

  private void roster(ClubActivity target, User user, boolean attended) {
    persist(ClubActivityAttendance.of(target, user, attended));
  }

  private static void assertError(ThrowingCallable call, ErrorCode expected) {
    assertThatThrownBy(call)
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(expected);
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  private <T> T persist(T entity) {
    entityManager.persist(entity);
    return entity;
  }

  private void join(User user) {
    ClubMember member = ClubMember.apply(club, user, null, JOINED);
    member.approve(JOINED);
    persist(member);
  }

  private User user(String name) {
    userSeq++;
    return persist(
        User.builder()
            .name(name)
            .oauthSubject("oauth-club-attendance-svc-" + userSeq)
            .major("CSE")
            .studentId("1226400" + userSeq)
            .phoneNumber("0104000000" + userSeq)
            .email("club-attendance-svc-" + userSeq + "@inha.edu")
            .build());
  }

  // ClubTerm 의 생성 메서드는 아직 없다(A·C 담당). 생기면 그걸로 바꾼다.
  private Club club(User clubLeader) {
    ClubTerm term = BeanUtils.instantiateClass(ClubTerm.class);
    ReflectionTestUtils.setField(term, "name", "2026-1");
    ReflectionTestUtils.setField(term, "attendanceRatio", new BigDecimal("0.50"));
    persist(term);
    return persist(
        Club.create(
            term,
            clubLeader,
            "출석 테스트",
            ClubCategory.STUDY,
            "소개",
            null,
            null,
            null,
            null,
            null,
            null,
            null));
  }
}
