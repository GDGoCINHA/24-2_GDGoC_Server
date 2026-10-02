package inha.gdgoc.domain.club.reaction.controller;

import inha.gdgoc.domain.club.reaction.dto.request.ClubCommentCreateRequest;
import inha.gdgoc.domain.club.reaction.dto.response.ClubCommentResponse;
import inha.gdgoc.domain.club.reaction.dto.response.ClubLikeResponse;
import inha.gdgoc.domain.club.reaction.enums.ClubTargetType;
import inha.gdgoc.domain.club.reaction.service.ClubReactionService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 좋아요·댓글. 부원(MEMBER 이상). {@code targetType} 은 POST 또는 ACTIVITY. */
@RestController
@RequiredArgsConstructor
@Authorize(@Condition(atLeast = UserRole.MEMBER))
public class ClubReactionController {

  private final ClubReactionService clubReactionService;

  @PutMapping("/api/v1/club-reactions/{targetType}/{targetId}/like")
  public ResponseEntity<ApiResponse<ClubLikeResponse, Void>> like(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable ClubTargetType targetType,
      @PathVariable long targetId) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_LIKED", clubReactionService.like(targetType, targetId, me.getUserId())));
  }

  @DeleteMapping("/api/v1/club-reactions/{targetType}/{targetId}/like")
  public ResponseEntity<ApiResponse<ClubLikeResponse, Void>> unlike(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable ClubTargetType targetType,
      @PathVariable long targetId) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_UNLIKED", clubReactionService.unlike(targetType, targetId, me.getUserId())));
  }

  @GetMapping("/api/v1/club-reactions/{targetType}/{targetId}/comments")
  public ResponseEntity<ApiResponse<List<ClubCommentResponse>, Void>> comments(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable ClubTargetType targetType,
      @PathVariable long targetId) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_COMMENT_LIST_RETRIEVED", clubReactionService.comments(targetType, targetId, me)));
  }

  @PostMapping("/api/v1/club-reactions/{targetType}/{targetId}/comments")
  public ResponseEntity<ApiResponse<Long, Void>> comment(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable ClubTargetType targetType,
      @PathVariable long targetId,
      @Valid @RequestBody ClubCommentCreateRequest req) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_COMMENT_CREATED",
            clubReactionService.comment(targetType, targetId, me.getUserId(), req.content())));
  }

  /** 작성자, 해당 팀 리더, 운영진. 소프트 삭제. */
  @DeleteMapping("/api/v1/club-comments/{commentId}")
  public ResponseEntity<ApiResponse<Void, Void>> deleteComment(
      @AuthenticationPrincipal CustomUserDetails me, @PathVariable Long commentId) {
    clubReactionService.deleteComment(commentId, me);
    return ResponseEntity.ok(ApiResponse.ok("CLUB_COMMENT_DELETED"));
  }
}
