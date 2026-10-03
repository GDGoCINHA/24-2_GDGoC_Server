package inha.gdgoc.domain.club.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import inha.gdgoc.domain.club.activity.dto.request.ClubActivitySubmitRequest;
import inha.gdgoc.domain.club.activity.dto.response.ClubActivityDetailResponse;
import inha.gdgoc.domain.club.activity.dto.response.ClubActivityRosterResponse;
import inha.gdgoc.domain.club.activity.entity.ClubActivity;
import inha.gdgoc.domain.club.activity.entity.ClubActivityAttendance;
import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import inha.gdgoc.domain.club.activity.exception.ClubActivityErrorCode;
import inha.gdgoc.domain.club.activity.repository.ClubActivityAttendanceRepository;
import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.entity.ClubTerm;
import inha.gdgoc.domain.club.club.enums.ClubCategory;
import inha.gdgoc.domain.club.club.enums.ClubStatus;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.domain.club.member.entity.ClubMember;
import inha.gdgoc.domain.club.schedule.entity.ClubSchedule;
import inha.gdgoc.domain.club.schedule.entity.ClubScheduleCheckin;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
 * 활동 기록 제출·수정·상세를 실제 DB 로 검증한다.
 *
 * <p>팀 구성 (10월, KST):
 *
 * <ul>
 *   <li>리더 — 1일 합류
 *   <li>민지 — 1일 합류
 *   <li>도윤 — 1일 합류, <b>10일 탈퇴</b>
 *   <li>서연 — <b>10일 중도 합류</b>
 *   <li>외부인 — 멤버 아님
 * </ul>
 *
 * 그래서 7일 명단은 {리더, 민지, 도윤}, 14일 명단은 {리더, 민지, 서연} 이다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ClubActivityServiceTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final LocalDate OCT_7 = LocalDate.of(2026, 10, 7);
  private static final LocalDate OCT_14 = LocalDate.of(2026, 10, 14);
  private static final Long REVIEWER_ID = 99L;

  @Autowired private ClubActivityService clubActivityService;
  @Autowired private ClubActivityAttendanceRepository attendanceRepository;
  @Autowired private EntityManager entityManager;

  private int userSeq;
  private ClubTerm term;
  private Club club;
  private User leader;
  private User minji;
  private User doyun;
  private User seoyeon;
  private User outsider;

  @BeforeEach
  void setUp() {
    leader = user("리더");
    minji = user("민지");
    doyun = user("도윤");
    seoyeon = user("서연");
    outsider = user("외부인");
    term = term("0.50");
    club = club(leader);

    persist(ClubMember.founder(club, leader, kst(1, 9)));
    join(minji, kst(1, 12));
    ClubMember leaving = join(doyun, kst(1, 12));
    leaving.leave(kst(10, 10));
    join(seoyeon, kst(10, 12));
    entityManager.flush();
  }

  // ---- 명단 ----

  @Test
  @DisplayName("출석 행은 활동일 명단 전원이고, 요청에서는 출석 여부만 가져온다")
  void createBuildsRosterFromActivityDate() {
    Long id = create(req(OCT_7, null, minji, seoyeon, outsider));

    // 서연(아직 합류 전)과 외부인은 요청에 있어도 명단 밖이라 무시된다.
    assertThat(attendance(id))
        .containsExactlyInAnyOrderEntriesOf(Map.of("리더", false, "민지", true, "도윤", false));
  }

  @Test
  @DisplayName("활동일을 바꾸면 새 날짜 기준으로 명단을 다시 만든다 — 탈퇴자는 빠지고 중도 합류자가 든다")
  void updateRebuildsRosterForNewDate() {
    Long id = create(req(OCT_7, null, minji, doyun));

    clubActivityService.update(club.getId(), id, leader.getId(), req(OCT_14, null, seoyeon));
    flushAndClear();

    assertThat(attendance(id))
        .containsExactlyInAnyOrderEntriesOf(Map.of("리더", false, "민지", false, "서연", true));
  }

  @Test
  @DisplayName("한 사람의 멤버 행이 그날 두 개 걸려도 출석 행은 하나다")
  void duplicateMemberRowsCollapse() {
    join(minji, kst(2, 12)); // 정상이라면 없을 겹친 행
    entityManager.flush();

    Long id = create(req(OCT_7, null, minji));

    assertThat(attendanceRepository.findAllWithUser(id)).hasSize(3);
  }

  @Test
  @DisplayName("명단 화면: 일정의 QR 체크인이 기본값으로 표시되고 필요 인원이 계산된다")
  void rosterShowsCheckinsAndRequiredCount() {
    ClubSchedule schedule = schedule(club);
    persist(ClubScheduleCheckin.of(schedule, minji, kst(7, 19)));
    entityManager.flush();

    ClubActivityRosterResponse roster =
        clubActivityService.roster(club.getId(), leader.getId(), OCT_7, schedule.getId());

    assertThat(roster.requiredCount()).isEqualTo(2); // ceil(3 × 0.5)
    assertThat(roster.members())
        .extracting(
            ClubActivityRosterResponse.Member::name,
            ClubActivityRosterResponse.Member::leader,
            ClubActivityRosterResponse.Member::checkedIn)
        .containsExactlyInAnyOrder(
            tuple("리더", true, false), tuple("민지", false, true), tuple("도윤", false, false));
  }

  // ---- 수정 잠금 ----

  @Test
  @DisplayName("인증 완료된 기록은 수정할 수 없고 출석도 그대로다")
  void approvedCannotBeUpdated() {
    Long id = create(req(OCT_7, null, minji));
    entityManager.find(ClubActivity.class, id).approve(REVIEWER_ID, kst(8, 9));
    flushAndClear();

    assertError(
        () -> clubActivityService.update(club.getId(), id, leader.getId(), req(OCT_7, null, doyun)),
        ClubActivityErrorCode.ACTIVITY_LOCKED);
    flushAndClear();
    assertThat(attendance(id)).containsEntry("민지", true).containsEntry("도윤", false);
  }

  @Test
  @DisplayName("보완 요청된 기록을 고치면 확인 중으로 돌아간다")
  void revisedUpdateGoesBackToPending() {
    Long id = create(req(OCT_7, null, minji));
    entityManager.find(ClubActivity.class, id).requestRevision(REVIEWER_ID, "사진이 흐려요", kst(8, 9));
    flushAndClear();

    clubActivityService.update(club.getId(), id, leader.getId(), req(OCT_7, null, minji, doyun));
    flushAndClear();

    assertThat(entityManager.find(ClubActivity.class, id).getStatus())
        .isEqualTo(ClubActivityStatus.PENDING);
  }

  @Test
  @DisplayName("수정하면 사진을 통째로 바꾸고 순서를 지킨다")
  void updateReplacesPhotosInOrder() {
    Long id = create(req(OCT_7, null, minji));

    clubActivityService.update(
        club.getId(),
        id,
        leader.getId(),
        new ClubActivitySubmitRequest(OCT_7, null, List.of("b.jpg", "a.jpg"), "수정", null, null));
    flushAndClear();

    assertThat(clubActivityService.detail(club.getId(), id, leader.getId(), false).photoUrls())
        .containsExactly("b.jpg", "a.jpg");
  }

  // ---- 권한 ----

  @Test
  @DisplayName("리더가 아니면 기록을 쓰거나 명단을 볼 수 없다")
  void nonLeaderIsForbidden() {
    assertError(
        () -> clubActivityService.create(club.getId(), minji.getId(), req(OCT_7, null)),
        ClubErrorCode.NOT_CLUB_LEADER);
    assertError(
        () -> clubActivityService.roster(club.getId(), minji.getId(), OCT_7, null),
        ClubErrorCode.NOT_CLUB_LEADER);
  }

  @Test
  @DisplayName("다른 소모임의 기록을 경로만 바꿔 열거나 고칠 수 없다")
  void otherClubActivityIsNotFound() {
    User otherLeader = user("다른리더");
    Club other = club(otherLeader);
    persist(ClubMember.founder(other, otherLeader, kst(1, 9)));
    Long id = create(req(OCT_7, null, minji));

    assertError(
        () -> clubActivityService.detail(other.getId(), id, otherLeader.getId(), false),
        ClubActivityErrorCode.ACTIVITY_NOT_FOUND);
    assertError(
        () -> clubActivityService.update(other.getId(), id, otherLeader.getId(), req(OCT_7, null)),
        ClubActivityErrorCode.ACTIVITY_NOT_FOUND);
  }

  // ---- 상세 가시성 ----

  @Test
  @DisplayName("비멤버에게는 출석 명단과 보완 사유 대신 인원 수만 보인다")
  void outsiderSeesCountsOnly() {
    Long id = create(req(OCT_7, null, minji, doyun));
    entityManager.find(ClubActivity.class, id).requestRevision(REVIEWER_ID, "사진이 흐려요", kst(8, 9));
    flushAndClear();

    ClubActivityDetailResponse detail =
        clubActivityService.detail(club.getId(), id, outsider.getId(), false);

    assertThat(detail.attendance()).isNull();
    assertThat(detail.revisionReason()).isNull();
    assertThat(detail.rosterCount()).isEqualTo(3);
    assertThat(detail.attendedCount()).isEqualTo(2);
    assertThat(detail.requiredCount()).isEqualTo(2);
    assertThat(detail.editable()).isFalse();
  }

  @Test
  @DisplayName("팀 멤버와 운영진은 출석 명단과 보완 사유를 본다")
  void memberAndStaffSeeRoster() {
    Long id = create(req(OCT_7, null, minji));
    entityManager.find(ClubActivity.class, id).requestRevision(REVIEWER_ID, "사진이 흐려요", kst(8, 9));
    flushAndClear();

    ClubActivityDetailResponse asMember =
        clubActivityService.detail(club.getId(), id, minji.getId(), false);
    ClubActivityDetailResponse asStaff =
        clubActivityService.detail(club.getId(), id, outsider.getId(), true);

    assertThat(asMember.attendance()).hasSize(3);
    assertThat(asMember.revisionReason()).isEqualTo("사진이 흐려요");
    assertThat(asMember.editable()).isFalse();
    assertThat(asStaff.attendance()).hasSize(3);
    assertThat(asStaff.revisionReason()).isEqualTo("사진이 흐려요");
  }

  @Test
  @DisplayName("리더는 인증 완료 전까지 수정 가능으로 표시된다")
  void leaderEditableUntilApproved() {
    Long id = create(req(OCT_7, null, minji));
    assertThat(clubActivityService.detail(club.getId(), id, leader.getId(), false).editable())
        .isTrue();

    entityManager.find(ClubActivity.class, id).approve(REVIEWER_ID, kst(8, 9));
    flushAndClear();

    assertThat(clubActivityService.detail(club.getId(), id, leader.getId(), false).editable())
        .isFalse();
  }

  @Test
  @DisplayName("숨김 소모임의 기록은 운영진과 팀 멤버만 연다")
  void hiddenClubIsNotFoundForOutsiders() {
    Long id = create(req(OCT_7, null, minji));
    entityManager.find(Club.class, club.getId()).updateByStaff(ClubStatus.HIDDEN, term);
    flushAndClear();

    assertError(
        () -> clubActivityService.detail(club.getId(), id, outsider.getId(), false),
        ClubErrorCode.CLUB_NOT_FOUND);
    assertThat(clubActivityService.detail(club.getId(), id, outsider.getId(), true)).isNotNull();
    assertThat(clubActivityService.detail(club.getId(), id, minji.getId(), false)).isNotNull();
  }

  // ---- 일정 연결 ----

  @Test
  @DisplayName("같은 일정에 기록을 두 개 만들 수 없다")
  void oneActivityPerSchedule() {
    ClubSchedule schedule = schedule(club);
    create(req(OCT_7, schedule.getId(), minji));

    assertError(
        () ->
            clubActivityService.create(
                club.getId(), leader.getId(), req(OCT_7, schedule.getId())),
        ClubActivityErrorCode.SCHEDULE_ALREADY_RECORDED);
  }

  @Test
  @DisplayName("자기 일정을 그대로 두고 고치는 것은 중복이 아니다")
  void keepingOwnScheduleOnUpdate() {
    ClubSchedule schedule = schedule(club);
    Long id = create(req(OCT_7, schedule.getId(), minji));

    clubActivityService.update(
        club.getId(), id, leader.getId(), req(OCT_7, schedule.getId(), doyun));
    flushAndClear();

    assertThat(attendance(id)).containsEntry("도윤", true);
  }

  @Test
  @DisplayName("일정 없는 기록은 같은 날 여러 개 만들 수 있다")
  void manyUnscheduledActivitiesOnSameDay() {
    Long first = create(req(OCT_7, null, minji));
    Long second = create(req(OCT_7, null, doyun));

    assertThat(first).isNotEqualTo(second);
  }

  @Test
  @DisplayName("다른 소모임의 일정은 연결할 수 없다")
  void otherClubScheduleIsNotFound() {
    User otherLeader = user("다른리더");
    ClubSchedule foreign = schedule(club(otherLeader));

    assertError(
        () ->
            clubActivityService.create(club.getId(), leader.getId(), req(OCT_7, foreign.getId())),
        ClubActivityErrorCode.SCHEDULE_NOT_FOUND);
  }

  // ---- 도우미 ----

  private Long create(ClubActivitySubmitRequest req) {
    Long id = clubActivityService.create(club.getId(), leader.getId(), req);
    flushAndClear();
    return id;
  }

  private Map<String, Boolean> attendance(Long activityId) {
    return attendanceRepository.findAllWithUser(activityId).stream()
        .collect(Collectors.toMap(a -> a.getUser().getName(), ClubActivityAttendance::isAttended));
  }

  private static ClubActivitySubmitRequest req(
      LocalDate date, Long scheduleId, User... attended) {
    return new ClubActivitySubmitRequest(
        date,
        scheduleId,
        List.of("https://cdn.example/club/activity/1.jpg"),
        "알고리즘 문제 풀이",
        Arrays.stream(attended).map(User::getId).toList(),
        null);
  }

  private static void assertError(ThrowingCallable call, ErrorCode expected) {
    assertThatThrownBy(call)
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(expected);
  }

  private static Instant kst(int day, int hour) {
    return ZonedDateTime.of(2026, 10, day, hour, 0, 0, 0, KST).toInstant();
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

  private ClubSchedule schedule(Club target) {
    ClubSchedule schedule =
        persist(ClubSchedule.create(target, "3주차", kst(7, 19), "하이테크 301", null, null, null));
    entityManager.flush();
    return schedule;
  }

  private User user(String name) {
    userSeq++;
    return persist(
        User.builder()
            .name(name)
            .oauthSubject("oauth-club-activity-svc-" + userSeq)
            .major("CSE")
            .studentId("1226100" + userSeq)
            .phoneNumber("0101000000" + userSeq)
            .email("club-activity-svc-" + userSeq + "@inha.edu")
            .build());
  }

  // ClubTerm 의 생성 메서드는 아직 없다(A·C 담당). 생기면 그걸로 바꾼다.
  private ClubTerm term(String ratio) {
    ClubTerm created = BeanUtils.instantiateClass(ClubTerm.class);
    ReflectionTestUtils.setField(created, "name", "2026-2");
    ReflectionTestUtils.setField(created, "attendanceRatio", new BigDecimal(ratio));
    return persist(created);
  }

  private Club club(User clubLeader) {
    return persist(
        Club.create(
            term,
            clubLeader,
            clubLeader.getName() + "의 스터디",
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
