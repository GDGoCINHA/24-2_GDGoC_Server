package inha.gdgoc.domain.club.feed.controller;

import inha.gdgoc.domain.club.feed.dto.response.ClubFeedItemResponse;
import inha.gdgoc.domain.club.feed.service.ClubFeedService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.dto.response.PageMeta;
import inha.gdgoc.global.security.AccessGuard;
import inha.gdgoc.global.security.AccessGuard.AccessCondition;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 팀 피드와 전체 활동 피드. MEMBER 이상이다 — 비로그인·GUEST 에게 활동 피드를 보이지 않는다.
 *
 * <p>목록이라 data 는 배열이고 페이지 정보는 meta 에 있다(소모임 목록 API 와 같은 형태).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Authorize(@Condition(atLeast = UserRole.MEMBER))
public class ClubFeedController {

  private final ClubFeedService clubFeedService;
  private final AccessGuard accessGuard;

  @GetMapping("/clubs/{clubId}/feed")
  public ResponseEntity<ApiResponse<List<ClubFeedItemResponse>, PageMeta>> teamFeed(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Page<ClubFeedItemResponse> result =
        clubFeedService.teamFeed(
            clubId,
            me.getUserId(),
            accessGuard.check(me, AccessCondition.atLeast(UserRole.CORE)),
            page,
            size);
    return ResponseEntity.ok(
        ApiResponse.ok("CLUB_FEED_RETRIEVED", result.getContent(), PageMeta.of(result)));
  }

  @GetMapping("/club-feed")
  public ResponseEntity<ApiResponse<List<ClubFeedItemResponse>, PageMeta>> globalFeed(
      @AuthenticationPrincipal CustomUserDetails me,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Page<ClubFeedItemResponse> result = clubFeedService.globalFeed(me.getUserId(), page, size);
    return ResponseEntity.ok(
        ApiResponse.ok("CLUB_GLOBAL_FEED_RETRIEVED", result.getContent(), PageMeta.of(result)));
  }
}
