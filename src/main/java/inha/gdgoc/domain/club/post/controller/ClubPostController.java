package inha.gdgoc.domain.club.post.controller;

import inha.gdgoc.domain.club.post.dto.request.ClubPostRequest;
import inha.gdgoc.domain.club.post.service.ClubPostService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.security.AccessGuard;
import inha.gdgoc.global.security.AccessGuard.AccessCondition;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 팀 일반 게시글. 전부 MEMBER 이상이다 — GUEST 는 소모임에 참여할 수 없다.
 *
 * <p>쓰기는 팀 멤버, 고치기는 작성자, 지우기는 작성자·리더·운영진(서비스가 검사). 읽기는 피드로 한다.
 */
@RestController
@RequestMapping("/api/v1/clubs/{clubId}/posts")
@RequiredArgsConstructor
@Authorize(@Condition(atLeast = UserRole.MEMBER))
public class ClubPostController {

  private final ClubPostService clubPostService;
  private final AccessGuard accessGuard;

  @PostMapping
  public ResponseEntity<ApiResponse<Long, Void>> create(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @Valid @RequestBody ClubPostRequest req) {
    return ResponseEntity.ok(
        ApiResponse.ok("CLUB_POST_CREATED", clubPostService.create(clubId, me.getUserId(), req)));
  }

  @PatchMapping("/{postId}")
  public ResponseEntity<ApiResponse<Void, Void>> update(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @PathVariable Long postId,
      @Valid @RequestBody ClubPostRequest req) {
    clubPostService.update(clubId, postId, me.getUserId(), req);
    return ResponseEntity.ok(ApiResponse.ok("CLUB_POST_UPDATED"));
  }

  @DeleteMapping("/{postId}")
  public ResponseEntity<ApiResponse<Void, Void>> delete(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @PathVariable Long postId) {
    clubPostService.delete(
        clubId,
        postId,
        me.getUserId(),
        accessGuard.check(me, AccessCondition.atLeast(UserRole.CORE)));
    return ResponseEntity.ok(ApiResponse.ok("CLUB_POST_DELETED"));
  }
}
