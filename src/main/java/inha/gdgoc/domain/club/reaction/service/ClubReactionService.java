package inha.gdgoc.domain.club.reaction.service;

import inha.gdgoc.domain.club.club.service.ClubAccessService;
import inha.gdgoc.domain.club.reaction.dto.response.ClubCommentResponse;
import inha.gdgoc.domain.club.reaction.dto.response.ClubLikeResponse;
import inha.gdgoc.domain.club.reaction.dto.response.ClubReactionSummary;
import inha.gdgoc.domain.club.reaction.entity.ClubComment;
import inha.gdgoc.domain.club.reaction.entity.ClubLike;
import inha.gdgoc.domain.club.reaction.enums.ClubTargetType;
import inha.gdgoc.domain.club.reaction.exception.ClubReactionErrorCode;
import inha.gdgoc.domain.club.reaction.repository.ClubCommentRepository;
import inha.gdgoc.domain.club.reaction.repository.ClubLikeRepository;
import inha.gdgoc.domain.club.reaction.repository.ClubReactionTargetRepository;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.security.AccessGuard;
import inha.gdgoc.global.security.AccessGuard.AccessCondition;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좋아요·댓글. 대상은 일반 게시글(POST)과 활동 기록(ACTIVITY)이고, 로그인한 부원 누구나 단다 — 활동 피드가 부원 전체에게 공개이기 때문이다.
 *
 * <p>좋아요·취소는 멱등이다. 두 번 눌러도, 이미 취소한 걸 다시 취소해도 오류가 아니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubReactionService {

  private final ClubLikeRepository clubLikeRepository;
  private final ClubCommentRepository clubCommentRepository;
  private final ClubReactionTargetRepository clubReactionTargetRepository;
  private final ClubAccessService clubAccessService;
  private final UserRepository userRepository;
  private final AccessGuard accessGuard;

  // --- 좋아요 ---------------------------------------------------------------

  @Transactional
  public ClubLikeResponse like(ClubTargetType type, long targetId, Long userId) {
    requireTarget(type, targetId);
    // 같은 사람이 동시에 두 번 누르면 둘째 요청은 유니크 제약에 걸려 실패한다. 행은 하나만 남으므로 데이터는 맞고,
    // 잡아서 삼키지 않는다 — PostgreSQL 은 제약 위반 뒤 트랜잭션을 무효로 만들어 이어지는 count 도 실패한다.
    if (!clubLikeRepository.existsByTargetTypeAndTargetIdAndUserId(type, targetId, userId)) {
      clubLikeRepository.save(ClubLike.of(type, targetId, userRepository.getReferenceById(userId)));
    }
    return new ClubLikeResponse(true, clubLikeRepository.countByTargetTypeAndTargetId(type, targetId));
  }

  @Transactional
  public ClubLikeResponse unlike(ClubTargetType type, long targetId, Long userId) {
    clubLikeRepository.deleteMine(type, targetId, userId);
    return new ClubLikeResponse(
        false, clubLikeRepository.countByTargetTypeAndTargetId(type, targetId));
  }

  // --- 댓글 ----------------------------------------------------------------

  public List<ClubCommentResponse> comments(
      ClubTargetType type, long targetId, CustomUserDetails me) {
    Long clubId = requireTarget(type, targetId);
    boolean staff = isStaff(me);
    boolean leader = me != null && clubAccessService.isLeader(clubId, me.getUserId());
    return clubCommentRepository.findAlive(type, targetId).stream()
        .map(
            c ->
                new ClubCommentResponse(
                    c.getId(),
                    c.getAuthor().getId(),
                    c.getAuthor().getName(),
                    c.getContent(),
                    c.getCreatedAt(),
                    staff || leader || isAuthor(c, me)))
        .toList();
  }

  @Transactional
  public Long comment(ClubTargetType type, long targetId, Long userId, String content) {
    requireTarget(type, targetId);
    return clubCommentRepository
        .save(
            ClubComment.create(
                type, targetId, userRepository.getReferenceById(userId), content.strip()))
        .getId();
  }

  /** 작성자, 대상이 속한 팀의 리더, 운영진. 대상 글이 이미 지워졌어도 작성자·운영진은 지울 수 있다. */
  @Transactional
  public void deleteComment(Long commentId, CustomUserDetails me) {
    ClubComment comment =
        clubCommentRepository
            .findById(commentId)
            .filter(c -> !c.isDeleted())
            .orElseThrow(() -> new BusinessException(ClubReactionErrorCode.COMMENT_NOT_FOUND));
    if (!isAuthor(comment, me) && !isStaff(me) && !isTargetLeader(comment, me)) {
      throw new BusinessException(ClubReactionErrorCode.COMMENT_DELETE_FORBIDDEN);
    }
    comment.softDelete(Instant.now());
  }

  // --- B 의 피드가 부른다 -------------------------------------------------------

  /** 목록 카드들의 반응 수를 한 번에. 없는 id 는 {@link ClubReactionSummary#EMPTY}. */
  public Map<Long, ClubReactionSummary> summarize(
      ClubTargetType type, Collection<Long> targetIds, Long userId) {
    if (targetIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, Long> likes = toCountMap(clubLikeRepository.countByTargets(type, targetIds));
    Map<Long, Long> comments = toCountMap(clubCommentRepository.countByTargets(type, targetIds));
    Set<Long> liked =
        userId == null
            ? Set.of()
            : new HashSet<>(clubLikeRepository.findLikedTargets(type, targetIds, userId));
    Map<Long, ClubReactionSummary> out = new HashMap<>();
    for (Long id : targetIds) {
      out.put(
          id,
          new ClubReactionSummary(
              likes.getOrDefault(id, 0L), comments.getOrDefault(id, 0L), liked.contains(id)));
    }
    return out;
  }

  // -------------------------------------------------------------------------

  /** 대상이 있으면 그 팀 id. 없거나 지운 게시글이면 404. */
  private Long requireTarget(ClubTargetType type, long targetId) {
    Optional<Long> clubId =
        switch (type) {
          case POST -> clubReactionTargetRepository.findPostClubId(targetId);
          case ACTIVITY -> clubReactionTargetRepository.findActivityClubId(targetId);
        };
    return clubId.orElseThrow(() -> new BusinessException(ClubReactionErrorCode.TARGET_NOT_FOUND));
  }

  private boolean isTargetLeader(ClubComment comment, CustomUserDetails me) {
    if (me == null) {
      return false;
    }
    Optional<Long> clubId =
        switch (comment.getTargetType()) {
          case POST -> clubReactionTargetRepository.findPostClubId(comment.getTargetId());
          case ACTIVITY -> clubReactionTargetRepository.findActivityClubId(comment.getTargetId());
        };
    return clubId.map(id -> clubAccessService.isLeader(id, me.getUserId())).orElse(false);
  }

  private boolean isStaff(CustomUserDetails me) {
    return accessGuard.check(me, AccessCondition.atLeast(UserRole.CORE));
  }

  private static boolean isAuthor(ClubComment c, CustomUserDetails me) {
    return me != null && me.getUserId().equals(c.getAuthor().getId());
  }

  private static Map<Long, Long> toCountMap(List<Object[]> rows) {
    Map<Long, Long> out = new HashMap<>();
    for (Object[] row : rows) {
      out.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
    }
    return out;
  }
}
