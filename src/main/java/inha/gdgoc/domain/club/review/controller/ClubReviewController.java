package inha.gdgoc.domain.club.review.controller;

import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import inha.gdgoc.domain.club.review.dto.request.ClubRevisionRequest;
import inha.gdgoc.domain.club.review.dto.response.ClubReviewItemResponse;
import inha.gdgoc.domain.club.review.service.ClubReviewService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 운영진(CORE 이상)의 활동 인증 검토. 인증 완료된 기록을 다시 검토하면 409. */
@RestController
@RequestMapping("/api/v1/admin/club-activities")
@RequiredArgsConstructor
@Authorize(@Condition(atLeast = UserRole.CORE))
public class ClubReviewController {

  private final ClubReviewService clubReviewService;

  /** {@code status} 를 비우면 PENDING. */
  @GetMapping
  public ResponseEntity<ApiResponse<List<ClubReviewItemResponse>, Void>> queue(
      @RequestParam(required = false) ClubActivityStatus status,
      @RequestParam(required = false) Long clubId) {
    return ResponseEntity.ok(
        ApiResponse.ok("CLUB_REVIEW_QUEUE_RETRIEVED", clubReviewService.findQueue(status, clubId)));
  }

  @PostMapping("/{activityId}/approve")
  public ResponseEntity<ApiResponse<Void, Void>> approve(
      @AuthenticationPrincipal CustomUserDetails me, @PathVariable Long activityId) {
    clubReviewService.approve(activityId, me.getUserId());
    return ResponseEntity.ok(ApiResponse.ok("CLUB_ACTIVITY_APPROVED"));
  }

  @PostMapping("/{activityId}/request-revision")
  public ResponseEntity<ApiResponse<Void, Void>> requestRevision(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long activityId,
      @Valid @RequestBody ClubRevisionRequest req) {
    clubReviewService.requestRevision(activityId, me.getUserId(), req.reason());
    return ResponseEntity.ok(ApiResponse.ok("CLUB_ACTIVITY_REVISION_REQUESTED"));
  }
}
