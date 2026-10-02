package inha.gdgoc.domain.club.feed.dto.response;

import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import inha.gdgoc.domain.club.post.enums.ClubPostCategory;
import inha.gdgoc.domain.club.reaction.enums.ClubTargetType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 피드 카드 한 장. 팀 피드는 게시글(POST)과 활동 기록(ACTIVITY)을 섞고, 전체 피드는 활동 기록만 담는다.
 *
 * <p>종류에 해당하지 않는 필드는 null 이고 응답에서 빠진다. {@code type}·{@code id} 는 그대로 C 의 좋아요·댓글 대상이 된다.
 *
 * <p>활동 기록 카드에는 <b>출석 명단이 없다</b> — 인원 수만. 명단은 상세에서 팀 멤버·운영진에게만 보인다.
 *
 * @param clubId 전체 피드에서만 — 어느 소모임의 기록인지
 * @param editable 게시글: 지금 사용자가 작성자인가
 * @param deletable 게시글: 작성자·리더·운영진인가
 */
public record ClubFeedItemResponse(
    ClubTargetType type,
    Long id,
    Instant createdAt,
    String content,
    long likeCount,
    long commentCount,
    boolean likedByMe,
    Long clubId,
    String clubName,
    LocalDate activityDate,
    ClubActivityStatus status,
    List<String> photoUrls,
    Long attendedCount,
    Long rosterCount,
    Long requiredCount,
    ClubPostCategory category,
    Long authorId,
    String authorName,
    List<String> imageUrls,
    Boolean editable,
    Boolean deletable) {}
