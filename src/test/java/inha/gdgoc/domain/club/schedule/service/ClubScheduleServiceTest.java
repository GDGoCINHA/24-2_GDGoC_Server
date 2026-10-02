package inha.gdgoc.domain.club.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inha.gdgoc.domain.club.activity.entity.ClubActivity;
import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.entity.ClubTerm;
import inha.gdgoc.domain.club.club.enums.ClubCategory;
import inha.gdgoc.domain.club.club.enums.ClubStatus;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.domain.club.member.entity.ClubMember;
import inha.gdgoc.domain.club.schedule.dto.request.ClubScheduleRequest;
import inha.gdgoc.domain.club.schedule.dto.response.ClubCheckinTokenResponse;
import inha.gdgoc.domain.club.schedule.dto.response.ClubScheduleResponse;
import inha.gdgoc.domain.club.schedule.entity.ClubSchedule;
import inha.gdgoc.domain.club.schedule.entity.ClubScheduleCheckin;
import inha.gdgoc.domain.club.schedule.entity.ClubScheduleRsvp;
import inha.gdgoc.domain.club.schedule.enums.ClubRsvp;
import inha.gdgoc.domain.club.schedule.exception.ClubScheduleErrorCode;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * 일정·참석 응답을 실제 DB 로 검증한다. 다가오는/지난 일정은 오늘 기준이라 시각을 지금에서 며칠 떨어뜨려 만든다.
 *
 * <p>팀: 리더, 민지, 지호(응답 안 함), 탈퇴자(어제 탈퇴). 외부인은 멤버가 아니다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ClubScheduleServiceTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final Instant NOW = Instant.now();

  @Autowired private ClubScheduleService clubScheduleService;
  @Autowired private ClubCheckinTokenService clubCheckinTokenService;
  @Autowired private EntityManager entityManager;

  private int userSeq;
  private ClubTerm term;
  private Club club;
  private User leader;
  private User minji;
  private User jiho;
  private User left;
  private User outsider;

  @BeforeEach
  void setUp() {
    leader = user("리더");
    minji = user("민지");
    jiho = user("지호");
    left = user("탈퇴자");
    outsider = user("외부인");
    term = term();
    club = club(leader);

    Instant joined = NOW.minus(Duration.ofDays(30));
    persist(ClubMember.founder(club, leader, joined));
    join(minji, joined);
    join(jiho, joined);
    join(left, joined).leave(NOW.minus(Duration.ofDays(1)));
    entityManager.flush();
  }

  // ---- 등록·수정·삭제 ----

  @Test
  @DisplayName("리더는 일정을 등록·수정한다. 빈 장소·링크는 비운다")
  void leaderCreatesAndUpdates() {
    Long id =
        clubScheduleService.create(
            club.getId(), leader.getId(), req("3주차", inDays(2), "하이테크 301", " "));
    clubScheduleService.update(
        club.getId(), id, leader.getId(), req("3주차 (장소 변경)", inDays(3), "60주년 203", null));
    flushAndClear();

    ClubSchedule saved = entityManager.find(ClubSchedule.class, id);
    assertThat(saved.getTitle()).isEqualTo("3주차 (장소 변경)");
    assertThat(saved.getLocation()).isEqualTo("60주년 203");
    assertThat(saved.getOnlineLink()).isNull();
  }

  @Test
  @DisplayName("리더가 아니면 일정을 등록·삭제하거나 QR 을 띄울 수 없다")
  void nonLeaderIsForbidden() {
    ClubSchedule schedule = schedule(club, inDays(2));

    assertError(
        () ->
            clubScheduleService.create(
                club.getId(), minji.getId(), req("몰래", inDays(1), null, null)),
        ClubErrorCode.NOT_CLUB_LEADER);
    assertError(
        () -> clubScheduleService.delete(club.getId(), schedule.getId(), minji.getId()),
        ClubErrorCode.NOT_CLUB_LEADER);
    assertError(
        () -> clubScheduleService.issueCheckinToken(schedule.getId(), minji.getId()),
        ClubErrorCode.NOT_CLUB_LEADER);
  }

  @Test
  @DisplayName("일정을 지우면 연결된 활동 기록은 남고 연결만 끊긴다. 응답·체크인은 함께 지워진다")
  void deleteDetachesActivity() {
    ClubSchedule schedule = schedule(club, inDays(-1));
    ClubActivity activity =
        persist(
            ClubActivity.create(
                club, schedule, LocalDate.now(KST), "모임", null, leader.getId(), NOW));
    persist(ClubScheduleRsvp.of(schedule, minji, ClubRsvp.ATTEND));
    persist(ClubScheduleCheckin.of(schedule, minji, NOW));
    flushAndClear();

    clubScheduleService.delete(club.getId(), schedule.getId(), leader.getId());
    flushAndClear();

    assertThat(entityManager.find(ClubSchedule.class, schedule.getId())).isNull();
    ClubActivity kept = entityManager.find(ClubActivity.class, activity.getId());
    assertThat(kept).isNotNull();
    assertThat(kept.getSchedule()).isNull();
    assertThat(count("ClubScheduleRsvp")).isZero();
    assertThat(count("ClubScheduleCheckin")).isZero();
  }

  @Test
  @DisplayName("다른 소모임의 일정을 경로만 바꿔 지울 수 없다")
  void otherClubScheduleIsNotFound() {
    User otherLeader = user("다른리더");
    Club other = club(otherLeader);
    ClubSchedule foreign = schedule(other, inDays(2));

    assertError(
        () -> clubScheduleService.delete(club.getId(), foreign.getId(), leader.getId()),
        ClubScheduleErrorCode.SCHEDULE_NOT_FOUND);
  }

  // ---- 목록 ----

  @Test
  @DisplayName("다가오는 일정은 가까운 순, 지난 일정은 최근 순이다. 오늘 일정은 다가오는 쪽이다")
  void upcomingAndPastSplit() {
    ClubSchedule far = schedule(club, inDays(5));
    ClubSchedule near = schedule(club, inDays(1));
    ClubSchedule todayEarly = schedule(club, todayStartPlusMinutes(1));
    ClubSchedule lastWeek = schedule(club, inDays(-7));
    ClubSchedule yesterday = schedule(club, inDays(-1));

    assertThat(ids(clubScheduleService.list(club.getId(), minji.getId(), false, false)))
        .containsExactly(todayEarly.getId(), near.getId(), far.getId());
    assertThat(ids(clubScheduleService.list(club.getId(), minji.getId(), false, true)))
        .containsExactly(yesterday.getId(), lastWeek.getId());
  }

  @Test
  @DisplayName("응답 집계는 지금 팀원만 센다 — 탈퇴자의 옛 응답은 빠지고 미응답은 음수가 되지 않는다")
  void rsvpCountsCurrentMembersOnly() {
    ClubSchedule schedule = schedule(club, inDays(2));
    persist(ClubScheduleRsvp.of(schedule, minji, ClubRsvp.ATTEND));
    persist(ClubScheduleRsvp.of(schedule, leader, ClubRsvp.ABSENT));
    persist(ClubScheduleRsvp.of(schedule, left, ClubRsvp.ATTEND));
    flushAndClear();

    ClubScheduleResponse row =
        clubScheduleService.list(club.getId(), minji.getId(), false, false).get(0);

    assertThat(row.attendCount()).isEqualTo(1);
    assertThat(row.absentCount()).isEqualTo(1);
    assertThat(row.noResponseCount()).isEqualTo(1); // 지호
    assertThat(row.myResponse()).isEqualTo(ClubRsvp.ATTEND);
  }

  @Test
  @DisplayName("온라인 링크는 팀 멤버와 운영진에게만 준다")
  void onlineLinkForInsidersOnly() {
    clubScheduleService.create(
        club.getId(),
        leader.getId(),
        req("온라인 모임", inDays(2), null, "https://meet.example/abc"));
    flushAndClear();

    assertThat(onlineLinkSeenBy(outsider, false)).isNull();
    assertThat(onlineLinkSeenBy(minji, false)).isEqualTo("https://meet.example/abc");
    assertThat(onlineLinkSeenBy(outsider, true)).isEqualTo("https://meet.example/abc");
  }

  @Test
  @DisplayName("숨김 소모임의 일정은 운영진과 팀 멤버만 본다")
  void hiddenClubIsNotFoundForOutsiders() {
    schedule(club, inDays(2));
    entityManager.find(Club.class, club.getId()).updateByStaff(ClubStatus.HIDDEN, term);
    flushAndClear();

    assertError(
        () -> clubScheduleService.list(club.getId(), outsider.getId(), false, false),
        ClubErrorCode.CLUB_NOT_FOUND);
    assertThat(clubScheduleService.list(club.getId(), outsider.getId(), true, false)).hasSize(1);
    assertThat(clubScheduleService.list(club.getId(), minji.getId(), false, false)).hasSize(1);
  }

  @Test
  @DisplayName("기록이 연결된 일정에는 그 기록 id 가, 없는 일정에는 null 이 나온다")
  void scheduleCarriesLinkedActivityId() {
    ClubSchedule recorded = schedule(club, inDays(-2));
    ClubSchedule notYet = schedule(club, inDays(-3));
    ClubActivity activity =
        persist(
            ClubActivity.create(
                club, recorded, LocalDate.now(KST), "모임", null, leader.getId(), NOW));
    flushAndClear();

    List<ClubScheduleResponse> past =
        clubScheduleService.list(club.getId(), minji.getId(), false, true);

    assertThat(past)
        .extracting(ClubScheduleResponse::id, ClubScheduleResponse::activityId)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(recorded.getId(), activity.getId()),
            org.assertj.core.groups.Tuple.tuple(notYet.getId(), null));
  }

  // ---- 참석 응답 ----

  @Test
  @DisplayName("다시 응답하면 바뀐다. 행은 하나다")
  void respondTwiceChangesAnswer() {
    ClubSchedule schedule = schedule(club, inDays(2));

    clubScheduleService.respond(schedule.getId(), minji.getId(), ClubRsvp.ATTEND);
    flushAndClear();
    clubScheduleService.respond(schedule.getId(), minji.getId(), ClubRsvp.ABSENT);
    flushAndClear();

    assertThat(count("ClubScheduleRsvp")).isEqualTo(1);
    assertThat(clubScheduleService.list(club.getId(), minji.getId(), false, false).get(0).myResponse())
        .isEqualTo(ClubRsvp.ABSENT);
  }

  @Test
  @DisplayName("팀원이 아니면 참석 응답을 할 수 없다")
  void outsiderCannotRespond() {
    ClubSchedule schedule = schedule(club, inDays(2));

    assertError(
        () -> clubScheduleService.respond(schedule.getId(), outsider.getId(), ClubRsvp.ATTEND),
        ClubErrorCode.NOT_CLUB_MEMBER);
  }

  // ---- QR ----

  @Test
  @DisplayName("리더가 받은 QR 토큰은 그 일정으로 풀린다")
  void leaderIssuesToken() {
    ClubSchedule schedule = schedule(club, inDays(0));

    ClubCheckinTokenResponse token =
        clubScheduleService.issueCheckinToken(schedule.getId(), leader.getId());

    assertThat(clubCheckinTokenService.verify(token.token())).contains(schedule.getId());
    assertThat(token.expiresInSeconds()).isPositive();
  }

  // ---- 도우미 ----

  private String onlineLinkSeenBy(User viewer, boolean staff) {
    return clubScheduleService.list(club.getId(), viewer.getId(), staff, false).get(0).onlineLink();
  }

  private static List<Long> ids(List<ClubScheduleResponse> rows) {
    return rows.stream().map(ClubScheduleResponse::id).toList();
  }

  private static ClubScheduleRequest req(
      String title, Instant startsAt, String location, String onlineLink) {
    return new ClubScheduleRequest(title, startsAt, location, onlineLink, null);
  }

  private static Instant inDays(int days) {
    return NOW.plus(Duration.ofDays(days));
  }

  private static Instant todayStartPlusMinutes(int minutes) {
    return LocalDate.now(KST).atStartOfDay(KST).toInstant().plus(Duration.ofMinutes(minutes));
  }

  private long count(String entity) {
    return entityManager
        .createQuery("select count(e) from " + entity + " e", Long.class)
        .getSingleResult();
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

  private ClubMember join(User user, Instant at) {
    ClubMember member = ClubMember.apply(club, user, null, at);
    member.approve(at);
    return persist(member);
  }

  private ClubSchedule schedule(Club target, Instant startsAt) {
    ClubSchedule schedule =
        persist(ClubSchedule.create(target, "모임", startsAt, "하이테크 301", null, null, null));
    entityManager.flush();
    return schedule;
  }

  private User user(String name) {
    userSeq++;
    return persist(
        User.builder()
            .name(name)
            .oauthSubject("oauth-club-schedule-svc-" + userSeq)
            .major("CSE")
            .studentId("1226200" + userSeq)
            .phoneNumber("0102000000" + userSeq)
            .email("club-schedule-svc-" + userSeq + "@inha.edu")
            .build());
  }

  // ClubTerm 의 생성 메서드는 아직 없다(A·C 담당). 생기면 그걸로 바꾼다.
  private ClubTerm term() {
    ClubTerm created = BeanUtils.instantiateClass(ClubTerm.class);
    ReflectionTestUtils.setField(created, "name", "2026-2");
    ReflectionTestUtils.setField(created, "attendanceRatio", new BigDecimal("0.50"));
    return persist(created);
  }

  private Club club(User clubLeader) {
    return persist(
        Club.create(
            term,
            clubLeader,
            clubLeader.getName() + "의 모임",
            ClubCategory.HOBBY,
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
