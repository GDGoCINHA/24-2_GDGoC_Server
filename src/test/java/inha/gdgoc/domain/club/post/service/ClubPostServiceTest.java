package inha.gdgoc.domain.club.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.entity.ClubTerm;
import inha.gdgoc.domain.club.club.enums.ClubCategory;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.domain.club.member.entity.ClubMember;
import inha.gdgoc.domain.club.post.dto.request.ClubPostRequest;
import inha.gdgoc.domain.club.post.entity.ClubPost;
import inha.gdgoc.domain.club.post.enums.ClubPostCategory;
import inha.gdgoc.domain.club.post.exception.ClubPostErrorCode;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
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

/** 팀 일반 게시글 권한. 쓰기 팀 멤버, 고치기 작성자, 지우기 작성자·리더·운영진. */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ClubPostServiceTest {

  private static final Instant JOINED = Instant.parse("2025-12-01T00:00:00Z");
  private static final ClubPostRequest REQ =
      new ClubPostRequest(ClubPostCategory.QUESTION, " 3번 풀이 공유해 주실 분? ", List.of("a.jpg"));

  @Autowired private ClubPostService clubPostService;
  @Autowired private EntityManager entityManager;

  private int userSeq;
  private Club club;
  private User leader;
  private User minji;
  private User doyun;
  private User outsider;

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
    flushAndClear();
  }

  @Test
  @DisplayName("팀 멤버는 글을 쓴다. 내용 앞뒤 공백은 지운다")
  void memberWrites() {
    Long id = clubPostService.create(club.getId(), minji.getId(), REQ);
    flushAndClear();

    ClubPost saved = entityManager.find(ClubPost.class, id);
    assertThat(saved.getContent()).isEqualTo("3번 풀이 공유해 주실 분?");
    assertThat(saved.getImageUrls()).containsExactly("a.jpg");
    assertThat(saved.getAuthor().getId()).isEqualTo(minji.getId());
  }

  @Test
  @DisplayName("팀 멤버가 아니면 쓸 수 없다")
  void outsiderCannotWrite() {
    assertError(
        () -> clubPostService.create(club.getId(), outsider.getId(), REQ),
        ClubErrorCode.NOT_CLUB_MEMBER);
  }

  @Test
  @DisplayName("작성자만 고친다 — 리더도 남의 글은 못 고친다")
  void onlyAuthorEdits() {
    Long id = clubPostService.create(club.getId(), minji.getId(), REQ);
    ClubPostRequest edit = new ClubPostRequest(ClubPostCategory.REVIEW, "후기로 바꿈", null);

    assertError(
        () -> clubPostService.update(club.getId(), id, leader.getId(), edit),
        ClubPostErrorCode.POST_EDIT_FORBIDDEN);

    clubPostService.update(club.getId(), id, minji.getId(), edit);
    flushAndClear();
    ClubPost saved = entityManager.find(ClubPost.class, id);
    assertThat(saved.getCategory()).isEqualTo(ClubPostCategory.REVIEW);
    assertThat(saved.getImageUrls()).isEmpty();
  }

  @Test
  @DisplayName("지우기는 작성자·리더·운영진만. 다른 멤버는 못 지운다")
  void deletePermissions() {
    Long byAuthor = clubPostService.create(club.getId(), minji.getId(), REQ);
    Long byLeader = clubPostService.create(club.getId(), minji.getId(), REQ);
    Long byStaff = clubPostService.create(club.getId(), minji.getId(), REQ);

    assertError(
        () -> clubPostService.delete(club.getId(), byAuthor, doyun.getId(), false),
        ClubPostErrorCode.POST_DELETE_FORBIDDEN);

    clubPostService.delete(club.getId(), byAuthor, minji.getId(), false);
    clubPostService.delete(club.getId(), byLeader, leader.getId(), false);
    clubPostService.delete(club.getId(), byStaff, outsider.getId(), true);
    flushAndClear();

    assertThat(entityManager.find(ClubPost.class, byAuthor).isDeleted()).isTrue();
    assertThat(entityManager.find(ClubPost.class, byLeader).isDeleted()).isTrue();
    assertThat(entityManager.find(ClubPost.class, byStaff).isDeleted()).isTrue();
  }

  @Test
  @DisplayName("지운 글과 다른 소모임의 글은 없는 것으로 본다")
  void deletedOrForeignIsNotFound() {
    Long id = clubPostService.create(club.getId(), minji.getId(), REQ);
    clubPostService.delete(club.getId(), id, minji.getId(), false);
    flushAndClear();

    assertError(
        () -> clubPostService.update(club.getId(), id, minji.getId(), REQ),
        ClubPostErrorCode.POST_NOT_FOUND);

    Long live = clubPostService.create(club.getId(), minji.getId(), REQ);
    Club other = club(outsider);
    flushAndClear();
    assertError(
        () -> clubPostService.delete(other.getId(), live, outsider.getId(), false),
        ClubPostErrorCode.POST_NOT_FOUND);
  }

  // ---- 도우미 ----

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
            .oauthSubject("oauth-club-post-svc-" + userSeq)
            .major("CSE")
            .studentId("1226600" + userSeq)
            .phoneNumber("0106000000" + userSeq)
            .email("club-post-svc-" + userSeq + "@inha.edu")
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
            clubLeader.getName() + "의 모임",
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
