package inha.gdgoc.domain.club.reaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import inha.gdgoc.domain.club.club.service.ClubAccessService;
import inha.gdgoc.domain.club.reaction.dto.response.ClubReactionSummary;
import inha.gdgoc.domain.club.reaction.entity.ClubComment;
import inha.gdgoc.domain.club.reaction.enums.ClubTargetType;
import inha.gdgoc.domain.club.reaction.exception.ClubReactionErrorCode;
import inha.gdgoc.domain.club.reaction.repository.ClubCommentRepository;
import inha.gdgoc.domain.club.reaction.repository.ClubLikeRepository;
import inha.gdgoc.domain.club.reaction.repository.ClubReactionTargetRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.security.AccessGuard;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/** 좋아요·댓글. 댓글 삭제 권한(작성자·팀 리더·운영진)은 가시성 경계라 하나씩 고정한다. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClubReactionServiceTest {

  private static final long POST_ID = 7L;
  private static final Long CLUB_ID = 1L;
  private static final Long AUTHOR_ID = 10L;
  private static final Long LEADER_ID = 20L;
  private static final Long OTHER_ID = 30L;
  private static final Long COMMENT_ID = 100L;

  @Mock private ClubLikeRepository clubLikeRepository;
  @Mock private ClubCommentRepository clubCommentRepository;
  @Mock private ClubReactionTargetRepository clubReactionTargetRepository;
  @Mock private ClubAccessService clubAccessService;
  @Mock private UserRepository userRepository;

  private ClubReactionService service;
  private ClubComment comment;

  @BeforeEach
  void setUp() {
    service =
        new ClubReactionService(
            clubLikeRepository,
            clubCommentRepository,
            clubReactionTargetRepository,
            clubAccessService,
            userRepository,
            new AccessGuard());
    User author = User.builder().name("작성자").build();
    ReflectionTestUtils.setField(author, "id", AUTHOR_ID);
    comment = ClubComment.create(ClubTargetType.POST, POST_ID, author, "댓글");
    given(clubCommentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));
    given(clubReactionTargetRepository.findPostClubId(POST_ID)).willReturn(Optional.of(CLUB_ID));
    given(clubAccessService.isLeader(CLUB_ID, LEADER_ID)).willReturn(true);
  }

  private static CustomUserDetails user(Long id, UserRole role) {
    return new CustomUserDetails(id, "u" + id, "s", List.of(), role, TeamType.HR);
  }

  @Nested
  @DisplayName("댓글 삭제")
  class DeleteComment {

    @Test
    @DisplayName("작성자는 지울 수 있다")
    void author() {
      service.deleteComment(COMMENT_ID, user(AUTHOR_ID, UserRole.MEMBER));
      assertThat(comment.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("대상 글이 속한 팀의 리더는 지울 수 있다")
    void leader() {
      service.deleteComment(COMMENT_ID, user(LEADER_ID, UserRole.MEMBER));
      assertThat(comment.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("운영진은 지울 수 있다")
    void staff() {
      service.deleteComment(COMMENT_ID, user(OTHER_ID, UserRole.CORE));
      assertThat(comment.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("그 밖의 부원은 403")
    void otherMemberIsForbidden() {
      assertThatThrownBy(() -> service.deleteComment(COMMENT_ID, user(OTHER_ID, UserRole.MEMBER)))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ClubReactionErrorCode.COMMENT_DELETE_FORBIDDEN);
      assertThat(comment.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("대상 글이 지워져 팀을 알 수 없으면 리더 권한은 없다 — 작성자·운영진만")
    void leaderLosesRightWhenTargetGone() {
      given(clubReactionTargetRepository.findPostClubId(POST_ID)).willReturn(Optional.empty());

      assertThatThrownBy(() -> service.deleteComment(COMMENT_ID, user(LEADER_ID, UserRole.MEMBER)))
          .isInstanceOf(BusinessException.class);
      service.deleteComment(COMMENT_ID, user(AUTHOR_ID, UserRole.MEMBER));
      assertThat(comment.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("이미 지운 댓글은 404")
    void alreadyDeleted() {
      comment.softDelete(java.time.Instant.now());

      assertThatThrownBy(() -> service.deleteComment(COMMENT_ID, user(AUTHOR_ID, UserRole.MEMBER)))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ClubReactionErrorCode.COMMENT_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("좋아요")
  class Like {

    @Test
    @DisplayName("없는 대상에는 좋아요할 수 없다 (404)")
    void missingTarget() {
      given(clubReactionTargetRepository.findActivityClubId(999L)).willReturn(Optional.empty());

      assertThatThrownBy(() -> service.like(ClubTargetType.ACTIVITY, 999L, OTHER_ID))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ClubReactionErrorCode.TARGET_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 눌렀으면 다시 저장하지 않는다 (멱등)")
    void idempotent() {
      given(clubLikeRepository.existsByTargetTypeAndTargetIdAndUserId(ClubTargetType.POST, POST_ID, OTHER_ID))
          .willReturn(true);
      given(clubLikeRepository.countByTargetTypeAndTargetId(ClubTargetType.POST, POST_ID))
          .willReturn(3L);

      assertThat(service.like(ClubTargetType.POST, POST_ID, OTHER_ID).likeCount()).isEqualTo(3L);
      verify(clubLikeRepository, never()).save(any());
    }
  }

  @Test
  @DisplayName("목록 요약: 반응이 없는 대상도 0 으로 채우고, 내가 누른 것만 likedByMe")
  void summarize() {
    List<Long> ids = List.of(1L, 2L);
    given(clubLikeRepository.countByTargets(ClubTargetType.ACTIVITY, ids))
        .willReturn(List.<Object[]>of(new Object[] {1L, 4L}));
    given(clubCommentRepository.countByTargets(ClubTargetType.ACTIVITY, ids))
        .willReturn(List.<Object[]>of(new Object[] {2L, 2L}));
    given(clubLikeRepository.findLikedTargets(ClubTargetType.ACTIVITY, ids, OTHER_ID))
        .willReturn(List.of(1L));

    Map<Long, ClubReactionSummary> r = service.summarize(ClubTargetType.ACTIVITY, ids, OTHER_ID);

    assertThat(r.get(1L)).isEqualTo(new ClubReactionSummary(4, 0, true));
    assertThat(r.get(2L)).isEqualTo(new ClubReactionSummary(0, 2, false));
  }
}
