package inha.gdgoc.domain.club.feed.service;

import inha.gdgoc.domain.club.activity.entity.ClubActivity;
import inha.gdgoc.domain.club.activity.entity.ClubActivityPhoto;
import inha.gdgoc.domain.club.activity.repository.ClubActivityAttendanceRepository;
import inha.gdgoc.domain.club.activity.repository.ClubActivityPhotoRepository;
import inha.gdgoc.domain.club.activity.repository.ClubActivityRepository;
import inha.gdgoc.domain.club.activity.service.RequiredAttendance;
import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.service.ClubAccessService;
import inha.gdgoc.domain.club.common.exception.ClubErrorCode;
import inha.gdgoc.domain.club.feed.dto.response.ClubFeedItemResponse;
import inha.gdgoc.domain.club.feed.repository.ClubFeedRepository;
import inha.gdgoc.domain.club.post.entity.ClubPost;
import inha.gdgoc.domain.club.post.repository.ClubPostRepository;
import inha.gdgoc.domain.club.reaction.dto.response.ClubReactionSummary;
import inha.gdgoc.domain.club.reaction.enums.ClubTargetType;
import inha.gdgoc.domain.club.reaction.service.ClubReactionService;
import inha.gdgoc.global.exception.BusinessException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 팀 피드(게시글 + 활동 기록)와 전체 활동 피드. 올라온 시각 최신순이다.
 *
 * <p>카드에는 출석 명단을 담지 않는다 — 인원 수만. 좋아요·댓글 수는 C 의 {@link ClubReactionService#summarize} 로 한 번에 붙인다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubFeedService {

  private static final int MAX_PAGE_SIZE = 50;

  private final ClubAccessService clubAccessService;
  private final ClubFeedRepository clubFeedRepository;
  private final ClubPostRepository clubPostRepository;
  private final ClubActivityRepository clubActivityRepository;
  private final ClubActivityPhotoRepository clubActivityPhotoRepository;
  private final ClubActivityAttendanceRepository clubActivityAttendanceRepository;
  private final ClubReactionService clubReactionService;

  /**
   * 한 소모임의 타임라인. 부원이면 누구나 본다(기획 2.2).
   *
   * <p>숨김 소모임은 운영진과 팀 멤버가 아니면 없는 것으로 본다(활동 기록 상세와 같은 규칙).
   */
  public Page<ClubFeedItemResponse> teamFeed(
      Long clubId, Long userId, boolean staff, int page, int size) {
    Club club = clubAccessService.getClub(clubId);
    boolean member = clubAccessService.isActiveMember(clubId, userId);
    if (!club.getStatus().isPublic() && !staff && !member) {
      throw new BusinessException(ClubErrorCode.CLUB_NOT_FOUND);
    }
    PageRequest pageable = pageOf(page, size);

    List<Object[]> order =
        clubFeedRepository.findTeamFeedPage(
            clubId, pageable.getPageSize(), pageable.getOffset());
    List<Long> postIds = idsOf(order, ClubTargetType.POST);
    List<Long> activityIds = idsOf(order, ClubTargetType.ACTIVITY);

    boolean canModerate = staff || clubAccessService.isLeader(clubId, userId);
    Map<Long, ClubFeedItemResponse> posts = postItems(postIds, userId, canModerate);
    Map<Long, ClubFeedItemResponse> activities =
        activityItems(
            activityIds.isEmpty() ? List.of() : clubActivityRepository.findAllWithClub(activityIds),
            userId,
            false);

    List<ClubFeedItemResponse> items = new ArrayList<>();
    for (Object[] row : order) {
      Long id = ((Number) row[1]).longValue();
      items.add(kindOf(row) == ClubTargetType.POST ? posts.get(id) : activities.get(id));
    }
    long total =
        clubFeedRepository.countPosts(clubId) + clubFeedRepository.countActivities(clubId);
    return new PageImpl<>(items.stream().filter(Objects::nonNull).toList(), pageable, total);
  }

  /** 모든 소모임의 활동 기록. 숨김 소모임은 빠진다. 상태는 가리지 않고 카드에 배지로 보인다. */
  public Page<ClubFeedItemResponse> globalFeed(Long userId, int page, int size) {
    Page<ClubActivity> activities = clubActivityRepository.findGlobalFeed(pageOf(page, size));
    Map<Long, ClubFeedItemResponse> items =
        activityItems(activities.getContent(), userId, true);
    return activities.map(a -> items.get(a.getId()));
  }

  private Map<Long, ClubFeedItemResponse> postItems(
      List<Long> postIds, Long userId, boolean canModerate) {
    if (postIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, ClubReactionSummary> reactions =
        clubReactionService.summarize(ClubTargetType.POST, postIds, userId);
    return clubPostRepository.findAllWithAuthor(postIds).stream()
        .collect(
            Collectors.toMap(
                ClubPost::getId,
                p -> {
                  ClubReactionSummary r =
                      reactions.getOrDefault(p.getId(), ClubReactionSummary.EMPTY);
                  boolean mine = p.isWrittenBy(userId);
                  return new ClubFeedItemResponse(
                      ClubTargetType.POST,
                      p.getId(),
                      p.getCreatedAt(),
                      p.getContent(),
                      r.likeCount(),
                      r.commentCount(),
                      r.likedByMe(),
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      p.getCategory(),
                      p.getAuthor().getId(),
                      p.getAuthor().getName(),
                      p.getImageUrls() == null ? List.of() : p.getImageUrls(),
                      mine,
                      mine || canModerate);
                }));
  }

  /** 기록 카드. 사진·인원 수·반응 수를 id 묶음으로 한 번씩 읽는다. */
  private Map<Long, ClubFeedItemResponse> activityItems(
      List<ClubActivity> activities, Long userId, boolean withClub) {
    if (activities.isEmpty()) {
      return Map.of();
    }
    List<Long> ids = activities.stream().map(ClubActivity::getId).toList();
    Map<Long, List<String>> photos =
        clubActivityPhotoRepository.findAllByActivityIds(ids).stream()
            .collect(
                Collectors.groupingBy(
                    p -> p.getActivity().getId(),
                    Collectors.mapping(ClubActivityPhoto::getUrl, Collectors.toList())));
    Map<Long, long[]> counts =
        clubActivityAttendanceRepository.countByActivityIds(ids).stream()
            .collect(
                Collectors.toMap(
                    row -> ((Number) row[0]).longValue(),
                    row ->
                        new long[] {((Number) row[1]).longValue(), ((Number) row[2]).longValue()}));
    Map<Long, ClubReactionSummary> reactions =
        clubReactionService.summarize(ClubTargetType.ACTIVITY, ids, userId);

    return activities.stream()
        .collect(
            Collectors.toMap(
                ClubActivity::getId,
                a -> {
                  long[] c = counts.getOrDefault(a.getId(), new long[] {0, 0});
                  ClubReactionSummary r =
                      reactions.getOrDefault(a.getId(), ClubReactionSummary.EMPTY);
                  Club club = a.getClub();
                  return new ClubFeedItemResponse(
                      ClubTargetType.ACTIVITY,
                      a.getId(),
                      a.getCreatedAt(),
                      a.getContent(),
                      r.likeCount(),
                      r.commentCount(),
                      r.likedByMe(),
                      withClub ? club.getId() : null,
                      withClub ? club.getName() : null,
                      a.getActivityDate(),
                      a.getStatus(),
                      photos.getOrDefault(a.getId(), List.of()),
                      c[1],
                      c[0],
                      RequiredAttendance.of(c[0], club.getTerm().getAttendanceRatio()),
                      null,
                      null,
                      null,
                      null,
                      null,
                      null);
                },
                (x, y) -> x));
  }

  private static List<Long> idsOf(Collection<Object[]> order, ClubTargetType kind) {
    return order.stream()
        .filter(row -> kindOf(row) == kind)
        .map(row -> ((Number) row[1]).longValue())
        .toList();
  }

  /** 네이티브 문자열 리터럴은 DB 에 따라 고정 길이로 채워질 수 있어 공백을 걷어 낸다. */
  private static ClubTargetType kindOf(Object[] row) {
    return ClubTargetType.valueOf(String.valueOf(row[0]).trim());
  }

  private static PageRequest pageOf(int page, int size) {
    return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
  }
}
