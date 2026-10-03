package inha.gdgoc.domain.club.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inha.gdgoc.domain.club.activity.entity.ClubActivity;
import inha.gdgoc.domain.club.activity.entity.ClubActivityAttendance;
import inha.gdgoc.domain.club.activity.entity.ClubActivityPhoto;
import inha.gdgoc.domain.club.activity.entity.ClubAttendanceFixRequest;
import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.entity.ClubTerm;
import inha.gdgoc.domain.club.club.enums.ClubCategory;
import inha.gdgoc.domain.club.club.enums.ClubStatus;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.domain.club.completion.entity.ClubCompletion;
import inha.gdgoc.domain.club.completion.entity.ClubRestWeek;
import inha.gdgoc.domain.club.member.entity.ClubMember;
import inha.gdgoc.domain.club.post.entity.ClubPost;
import inha.gdgoc.domain.club.post.enums.ClubPostCategory;
import inha.gdgoc.domain.club.reaction.entity.ClubComment;
import inha.gdgoc.domain.club.reaction.entity.ClubLike;
import inha.gdgoc.domain.club.reaction.enums.ClubTargetType;
import inha.gdgoc.domain.club.schedule.entity.ClubSchedule;
import inha.gdgoc.domain.club.schedule.entity.ClubScheduleCheckin;
import inha.gdgoc.domain.club.schedule.entity.ClubScheduleRsvp;
import inha.gdgoc.domain.club.schedule.enums.ClubRsvp;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/** 소모임 삭제를 실제 DB 로 검증한다. 딸린 표 하나라도 순서가 틀리면 FK 에 걸리므로 목으로는 확인되지 않는다. */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ClubDeleteServiceTest {

  private static final Instant T0 = Instant.parse("2026-01-10T00:00:00Z");

  /** 소모임에 딸린 표. 삭제 뒤 대상 소모임 몫이 모두 0 이어야 한다. */
  private static final List<String> CHILD_COUNTS =
      List.of(
          "select count(*) from club_member where club_id = :id",
          "select count(*) from club_post where club_id = :id",
          "select count(*) from club_activity where club_id = :id",
          "select count(*) from club_schedule where club_id = :id",
          "select count(*) from club_completion where club_id = :id",
          "select count(*) from club_rest_week where club_id = :id");

  @Autowired private ClubDeleteService clubDeleteService;
  @Autowired private EntityManager entityManager;

  private int userSeq;
  private ClubTerm term;
  private User leader;
  private User member;

  @BeforeEach
  void setUp() {
    leader = user("리더");
    member = user("팀원");
    term = term();
  }

  @Test
  @DisplayName("리더는 승인 대기 소모임을 지운다")
  void leaderDeletesPending() {
    Club club = club(ClubStatus.PENDING);
    flushAndClear();

    clubDeleteService.deleteByLeader(club.getId(), leader.getId());

    assertThat(entityManager.find(Club.class, club.getId())).isNull();
  }

  @Test
  @DisplayName("리더는 반려된 소모임을 지운다")
  void leaderDeletesRejected() {
    Club club = club(ClubStatus.REJECTED);
    flushAndClear();

    clubDeleteService.deleteByLeader(club.getId(), leader.getId());

    assertThat(entityManager.find(Club.class, club.getId())).isNull();
  }

  @Test
  @DisplayName("리더는 공개된 소모임을 지울 수 없다")
  void leaderCannotDeleteActive() {
    Club club = club(ClubStatus.ACTIVE);
    flushAndClear();

    assertError(
        () -> clubDeleteService.deleteByLeader(club.getId(), leader.getId()),
        ClubErrorCode.CLUB_DELETE_NOT_ALLOWED);
    assertThat(entityManager.find(Club.class, club.getId())).isNotNull();
  }

  @Test
  @DisplayName("리더가 아니면 승인 대기 소모임도 지울 수 없다")
  void nonLeaderCannotDelete() {
    Club club = club(ClubStatus.PENDING);
    flushAndClear();

    assertError(
        () -> clubDeleteService.deleteByLeader(club.getId(), member.getId()),
        ClubErrorCode.NOT_CLUB_LEADER);
  }

  @Test
  @DisplayName("운영진은 공개된 소모임을 딸린 기록까지 지운다. 다른 소모임은 그대로다")
  void staffDeletesActiveWithEverything() {
    Club target = club(ClubStatus.ACTIVE);
    fill(target);
    Club other = club(ClubStatus.ACTIVE);
    fill(other);
    flushAndClear();

    clubDeleteService.deleteByStaff(target.getId());
    flushAndClear();

    assertThat(entityManager.find(Club.class, target.getId())).isNull();
    for (String sql : CHILD_COUNTS) {
      assertThat(count(sql, target.getId())).as(sql).isZero();
      assertThat(count(sql, other.getId())).as(sql).isPositive();
    }
    assertThat(countReactions()).isEqualTo(4); // 남은 소모임의 좋아요·댓글 2+2
  }

  @Test
  @DisplayName("없는 소모임은 404")
  void missingClubIsNotFound() {
    assertError(() -> clubDeleteService.deleteByStaff(-1L), ClubErrorCode.CLUB_NOT_FOUND);
  }

  /** 소모임에 딸린 표마다 한 줄 이상 넣는다. 좋아요·댓글은 게시글·활동 기록에 하나씩. */
  private void fill(Club club) {
    ClubMember joined = ClubMember.apply(club, member, null, T0);
    joined.approve(T0);
    persist(joined);
    ClubSchedule schedule =
        persist(ClubSchedule.create(club, "정기 모임", T0, "하이테크관", null, null, leader.getId()));
    persist(ClubScheduleRsvp.of(schedule, member, ClubRsvp.ATTEND));
    persist(ClubScheduleCheckin.of(schedule, member, T0));
    ClubActivity activity =
        persist(
            ClubActivity.create(
                club, schedule, LocalDate.of(2026, 1, 7), "모임", null, leader.getId(), T0));
    persist(ClubActivityAttendance.of(activity, member, true));
    persist(ClubActivityPhoto.of(activity, "https://example.com/a.jpg", 0));
    persist(ClubAttendanceFixRequest.create(activity, member, false, "착오"));
    ClubPost post = persist(ClubPost.create(club, member, ClubPostCategory.QUESTION, "질문", List.of()));
    persist(ClubLike.of(ClubTargetType.POST, post.getId(), leader));
    persist(ClubComment.create(ClubTargetType.POST, post.getId(), leader, "답"));
    persist(ClubLike.of(ClubTargetType.ACTIVITY, activity.getId(), member));
    persist(ClubComment.create(ClubTargetType.ACTIVITY, activity.getId(), member, "좋아요"));
    persist(ClubCompletion.create(club));
    persist(ClubRestWeek.create(club, LocalDate.of(2026, 1, 12)));
  }

  private long count(String sql, Long clubId) {
    return ((Number) entityManager.createNativeQuery(sql).setParameter("id", clubId).getSingleResult())
        .longValue();
  }

  private long countReactions() {
    long likes = ((Number) entityManager.createNativeQuery("select count(*) from club_like").getSingleResult()).longValue();
    long comments =
        ((Number) entityManager.createNativeQuery("select count(*) from club_comment").getSingleResult())
            .longValue();
    return likes + comments;
  }

  private static void assertError(Runnable call, ClubErrorCode code) {
    assertThatThrownBy(call::run)
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(code);
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  private <T> T persist(T entity) {
    entityManager.persist(entity);
    return entity;
  }

  private User user(String name) {
    userSeq++;
    return persist(
        User.builder()
            .name(name)
            .oauthSubject("oauth-club-delete-" + userSeq)
            .major("CSE")
            .studentId("1226600" + userSeq)
            .phoneNumber("0106000000" + userSeq)
            .email("club-delete-" + userSeq + "@inha.edu")
            .build());
  }

  private ClubTerm term() {
    ClubTerm created = BeanUtils.instantiateClass(ClubTerm.class);
    ReflectionTestUtils.setField(created, "name", "2026-1");
    ReflectionTestUtils.setField(created, "attendanceRatio", new BigDecimal("0.50"));
    return persist(created);
  }

  private Club club(ClubStatus status) {
    Club club =
        Club.create(
            term, leader, "소모임", ClubCategory.STUDY, "소개", null, null, null, null, null, null, null);
    ReflectionTestUtils.setField(club, "status", status);
    persist(club);
    persist(ClubMember.founder(club, leader, T0));
    return club;
  }
}
