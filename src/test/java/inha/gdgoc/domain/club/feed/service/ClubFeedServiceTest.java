package inha.gdgoc.domain.club.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import inha.gdgoc.domain.club.activity.entity.ClubActivity;
import inha.gdgoc.domain.club.activity.entity.ClubActivityAttendance;
import inha.gdgoc.domain.club.activity.entity.ClubActivityPhoto;
import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.entity.ClubTerm;
import inha.gdgoc.domain.club.club.enums.ClubCategory;
import inha.gdgoc.domain.club.club.enums.ClubStatus;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.domain.club.feed.dto.response.ClubFeedItemResponse;
import inha.gdgoc.domain.club.member.entity.ClubMember;
import inha.gdgoc.domain.club.post.entity.ClubPost;
import inha.gdgoc.domain.club.post.enums.ClubPostCategory;
import inha.gdgoc.domain.club.reaction.enums.ClubTargetType;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * 팀 피드·전체 피드를 실제 DB 로 검증한다. 팀 피드의 순서는 네이티브 UNION 쿼리라 목으로는 확인되지 않는다.
 *
 * <p>같은 밀리초에 만든 행은 순서가 흔들리므로 {@code created_at} 을 직접 정해 둔다.
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ClubFeedServiceTest {

  private static final Instant T0 = Instant.parse("2026-01-10T00:00:00Z");
  private static final LocalDate DAY = LocalDate.of(2026, 1, 7);

  @Autowired private ClubFeedService clubFeedService;
  @Autowired private EntityManager entityManager;

  private int userSeq;
  private ClubTerm term;
  private Club club;
  private User leader;
  private User minji;
  private User outsider;

  @BeforeEach
  void setUp() {
    leader = user("리더");
    minji = user("민지");
    outsider = user("외부인");
    term = term();
    club = club(leader, "알고리즘 스터디");
    persist(ClubMember.founder(club, leader, T0.minusSeconds(86400 * 30)));
    ClubMember joining = ClubMember.apply(club, minji, null, T0.minusSeconds(86400 * 30));
    joining.approve(T0.minusSeconds(86400 * 30));
    persist(joining);
    flushAndClear();
  }

  @Test
  @DisplayName("팀 피드는 게시글과 활동 기록을 올라온 시각 최신순으로 섞는다. 지운 글은 빠진다")
  void teamFeedMixesNewestFirst() {
    ClubPost oldPost = post(minji, "첫 질문", 1);
    ClubActivity activity = activity(club, DAY, ClubActivityStatus.PENDING, 2);
    ClubPost newPost = post(leader, "공지", 3);
    ClubPost deleted = post(minji, "지울 글", 4);
    deleted.softDelete(T0);
    flushAndClear();

    Page<ClubFeedItemResponse> page =
        clubFeedService.teamFeed(club.getId(), minji.getId(), false, 0, 20);

    assertThat(page.getContent())
        .extracting(ClubFeedItemResponse::type, ClubFeedItemResponse::id)
        .containsExactly(
            tuple(ClubTargetType.POST, newPost.getId()),
            tuple(ClubTargetType.ACTIVITY, activity.getId()),
            tuple(ClubTargetType.POST, oldPost.getId()));
    assertThat(page.getTotalElements()).isEqualTo(3);
  }

  @Test
  @DisplayName("팀 피드는 페이지로 나뉜다")
  void teamFeedPages() {
    for (int i = 1; i <= 5; i++) {
      post(minji, "글 " + i, i);
    }
    flushAndClear();

    Page<ClubFeedItemResponse> second =
        clubFeedService.teamFeed(club.getId(), minji.getId(), false, 1, 2);

    assertThat(second.getContent())
        .extracting(ClubFeedItemResponse::content)
        .containsExactly("글 3", "글 2");
    assertThat(second.getTotalElements()).isEqualTo(5);
    assertThat(second.getTotalPages()).isEqualTo(3);
  }

  @Test
  @DisplayName("활동 기록 카드에는 인원 수·필요 인원·사진이 있고 명단은 없다")
  void activityCardCountsOnly() {
    ClubActivity activity = activity(club, DAY, ClubActivityStatus.APPROVED, 1);
    persist(ClubActivityAttendance.of(activity, leader, true));
    persist(ClubActivityAttendance.of(activity, minji, false));
    persist(ClubActivityPhoto.of(activity, "b.jpg", 1));
    persist(ClubActivityPhoto.of(activity, "a.jpg", 0));
    flushAndClear();

    ClubFeedItemResponse card =
        clubFeedService
            .teamFeed(club.getId(), outsider.getId(), false, 0, 20)
            .getContent()
            .get(0);

    assertThat(card.type()).isEqualTo(ClubTargetType.ACTIVITY);
    assertThat(card.rosterCount()).isEqualTo(2);
    assertThat(card.attendedCount()).isEqualTo(1);
    assertThat(card.requiredCount()).isEqualTo(1); // ceil(2 × 0.5)
    assertThat(card.photoUrls()).containsExactly("a.jpg", "b.jpg");
    assertThat(card.status()).isEqualTo(ClubActivityStatus.APPROVED);
    assertThat(card.clubName()).isNull(); // 팀 피드에서는 소모임을 따로 싣지 않는다
  }

  @Test
  @DisplayName("게시글 카드: 작성자는 고치고 지우며, 리더는 지우기만, 다른 사람은 둘 다 못 한다")
  void postCardPermissions() {
    post(minji, "질문", 1);
    flushAndClear();

    ClubFeedItemResponse asAuthor = onlyCard(minji, false);
    ClubFeedItemResponse asLeader = onlyCard(leader, false);
    ClubFeedItemResponse asOutsider = onlyCard(outsider, false);
    ClubFeedItemResponse asStaff = onlyCard(outsider, true);

    assertThat(asAuthor.editable()).isTrue();
    assertThat(asAuthor.deletable()).isTrue();
    assertThat(asLeader.editable()).isFalse();
    assertThat(asLeader.deletable()).isTrue();
    assertThat(asOutsider.editable()).isFalse();
    assertThat(asOutsider.deletable()).isFalse();
    assertThat(asStaff.deletable()).isTrue();
    assertThat(asAuthor.authorName()).isEqualTo("민지");
    assertThat(asAuthor.category()).isEqualTo(ClubPostCategory.QUESTION);
  }

  @Test
  @DisplayName("숨김 소모임의 팀 피드는 운영진과 팀 멤버만 본다")
  void hiddenTeamFeed() {
    entityManager.find(Club.class, club.getId()).updateByStaff(ClubStatus.HIDDEN, term);
    flushAndClear();

    assertThatThrownBy(
            () -> clubFeedService.teamFeed(club.getId(), outsider.getId(), false, 0, 20))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ClubErrorCode.CLUB_NOT_FOUND);
    assertThat(clubFeedService.teamFeed(club.getId(), minji.getId(), false, 0, 20)).isNotNull();
    assertThat(clubFeedService.teamFeed(club.getId(), outsider.getId(), true, 0, 20)).isNotNull();
  }

  @Test
  @DisplayName("전체 피드는 모든 소모임의 활동 기록을 최신순으로, 숨김 소모임은 빼고, 소모임 이름과 함께 보인다")
  void globalFeed() {
    User otherLeader = user("다른리더");
    Club other = club(otherLeader, "러닝 크루");
    Club hidden = club(otherLeader, "숨김 모임");
    ClubActivity mine = activity(club, DAY, ClubActivityStatus.REVISION_REQUESTED, 1);
    ClubActivity theirs = activity(other, DAY, ClubActivityStatus.PENDING, 2);
    activity(hidden, DAY, ClubActivityStatus.APPROVED, 3);
    post(minji, "게시글은 전체 피드에 안 나온다", 4);
    flushAndClear();
    entityManager.find(Club.class, hidden.getId()).updateByStaff(ClubStatus.HIDDEN, term);
    flushAndClear();

    Page<ClubFeedItemResponse> page = clubFeedService.globalFeed(minji.getId(), 0, 20);

    assertThat(page.getContent())
        .extracting(
            ClubFeedItemResponse::id, ClubFeedItemResponse::clubName, ClubFeedItemResponse::status)
        .containsExactly(
            tuple(theirs.getId(), "러닝 크루", ClubActivityStatus.PENDING),
            tuple(mine.getId(), "알고리즘 스터디", ClubActivityStatus.REVISION_REQUESTED));
  }

  // ---- 도우미 ----

  private ClubFeedItemResponse onlyCard(User viewer, boolean staff) {
    List<ClubFeedItemResponse> content =
        clubFeedService.teamFeed(club.getId(), viewer.getId(), staff, 0, 20).getContent();
    assertThat(content).hasSize(1);
    return content.get(0);
  }

  /** {@code minutes} 분 뒤에 올라온 것으로 둔다. */
  private ClubPost post(User author, String content, int minutes) {
    ClubPost post =
        persist(ClubPost.create(club, author, ClubPostCategory.QUESTION, content, List.of()));
    entityManager.flush();
    setCreatedAt("club_post", post.getId(), minutes);
    return post;
  }

  private ClubActivity activity(
      Club target, LocalDate date, ClubActivityStatus status, int minutes) {
    ClubActivity activity =
        persist(
            ClubActivity.create(target, null, date, "모임 " + minutes, null, leader.getId(), T0));
    if (status == ClubActivityStatus.APPROVED) {
      activity.approve(99L, T0);
    } else if (status == ClubActivityStatus.REVISION_REQUESTED) {
      activity.requestRevision(99L, "보완", T0);
    }
    entityManager.flush();
    setCreatedAt("club_activity", activity.getId(), minutes);
    return activity;
  }

  private void setCreatedAt(String table, Long id, int minutes) {
    entityManager
        .createNativeQuery("update " + table + " set created_at = ?1 where id = ?2")
        .setParameter(1, Timestamp.from(T0.plusSeconds(60L * minutes)))
        .setParameter(2, id)
        .executeUpdate();
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
            .oauthSubject("oauth-club-feed-svc-" + userSeq)
            .major("CSE")
            .studentId("1226500" + userSeq)
            .phoneNumber("0105000000" + userSeq)
            .email("club-feed-svc-" + userSeq + "@inha.edu")
            .build());
  }

  // ClubTerm 의 생성 메서드는 아직 없다(A·C 담당). 생기면 그걸로 바꾼다.
  private ClubTerm term() {
    ClubTerm created = BeanUtils.instantiateClass(ClubTerm.class);
    ReflectionTestUtils.setField(created, "name", "2026-1");
    ReflectionTestUtils.setField(created, "attendanceRatio", new BigDecimal("0.50"));
    return persist(created);
  }

  private Club club(User clubLeader, String name) {
    // 개설 직후는 승인 대기(PENDING)다. 공개된 소모임으로 테스트한다.
    Club club =
        Club.create(
            term,
            clubLeader,
            name,
            ClubCategory.STUDY,
            "소개",
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    club.approve();
    return persist(club);
  }
}
