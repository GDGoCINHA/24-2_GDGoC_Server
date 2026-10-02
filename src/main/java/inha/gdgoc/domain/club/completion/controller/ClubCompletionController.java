package inha.gdgoc.domain.club.completion.controller;

import inha.gdgoc.domain.club.completion.dto.request.ClubCompletionDecisionRequest;
import inha.gdgoc.domain.club.completion.dto.request.ClubGoalRequest;
import inha.gdgoc.domain.club.completion.dto.request.ClubGoalResultRequest;
import inha.gdgoc.domain.club.completion.dto.request.ClubGoalStatusRequest;
import inha.gdgoc.domain.club.completion.dto.request.ClubRestWeeksRequest;
import inha.gdgoc.domain.club.completion.dto.response.ClubCompletionResponse;
import inha.gdgoc.domain.club.completion.service.ClubCompletionService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 목표·쉬는 주·완주.
 *
 * <p>역할 검사(MEMBER·CORE)는 여기서, 「이 팀의 리더인가」는 서비스에서 본다.
 */
@RestController
@RequiredArgsConstructor
public class ClubCompletionController {

  private final ClubCompletionService clubCompletionService;

  @Authorize(@Condition(atLeast = UserRole.MEMBER))
  @GetMapping("/api/v1/clubs/{clubId}/completion")
  public ResponseEntity<ApiResponse<ClubCompletionResponse, Void>> completion(
      @PathVariable Long clubId) {
    return ResponseEntity.ok(
        ApiResponse.ok("CLUB_COMPLETION_RETRIEVED", clubCompletionService.getCompletion(clubId)));
  }

  /** 리더 또는 운영진. */
  @Authorize(@Condition(atLeast = UserRole.MEMBER))
  @PutMapping("/api/v1/clubs/{clubId}/rest-weeks")
  public ResponseEntity<ApiResponse<List<LocalDate>, Void>> restWeeks(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @Valid @RequestBody ClubRestWeeksRequest req) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_REST_WEEKS_UPDATED",
            clubCompletionService.replaceRestWeeks(clubId, me, req.weeks())));
  }

  /** 리더. */
  @Authorize(@Condition(atLeast = UserRole.MEMBER))
  @PutMapping("/api/v1/clubs/{clubId}/goal")
  public ResponseEntity<ApiResponse<Void, Void>> goal(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @Valid @RequestBody ClubGoalRequest req) {
    clubCompletionService.updateGoal(clubId, me.getUserId(), req);
    return ResponseEntity.ok(ApiResponse.ok("CLUB_GOAL_UPDATED"));
  }

  /** 리더. 제출하면 목표 상태가 SUBMITTED 가 된다. */
  @Authorize(@Condition(atLeast = UserRole.MEMBER))
  @PutMapping("/api/v1/clubs/{clubId}/goal-result")
  public ResponseEntity<ApiResponse<Void, Void>> goalResult(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @Valid @RequestBody ClubGoalResultRequest req) {
    clubCompletionService.submitGoalResult(clubId, me.getUserId(), req);
    return ResponseEntity.ok(ApiResponse.ok("CLUB_GOAL_RESULT_SUBMITTED"));
  }

  @Authorize(@Condition(atLeast = UserRole.CORE))
  @PostMapping("/api/v1/admin/clubs/{clubId}/goal-status")
  public ResponseEntity<ApiResponse<Void, Void>> goalStatus(
      @PathVariable Long clubId, @Valid @RequestBody ClubGoalStatusRequest req) {
    clubCompletionService.judgeGoal(clubId, req.status());
    return ResponseEntity.ok(ApiResponse.ok("CLUB_GOAL_JUDGED"));
  }

  /** 계산값과 무관하게 확정한다. 미충족인데 COMPLETED 로 확정하는지는 화면에서 한 번 더 묻는다. */
  @Authorize(@Condition(atLeast = UserRole.CORE))
  @PostMapping("/api/v1/admin/clubs/{clubId}/completion")
  public ResponseEntity<ApiResponse<Void, Void>> confirm(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @Valid @RequestBody ClubCompletionDecisionRequest req) {
    clubCompletionService.confirm(clubId, me.getUserId(), req);
    return ResponseEntity.ok(ApiResponse.ok("CLUB_COMPLETION_CONFIRMED"));
  }
}
