package inha.gdgoc.domain.club.activity.controller;

import inha.gdgoc.domain.club.activity.dto.request.ClubFixRequestCreateRequest;
import inha.gdgoc.domain.club.activity.dto.response.ClubFixRequestResponse;
import inha.gdgoc.domain.club.activity.dto.response.MyAttendanceResponse;
import inha.gdgoc.domain.club.activity.enums.ClubFixRequestStatus;
import inha.gdgoc.domain.club.activity.service.ClubAttendanceService;
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

/**
 * 내 출석과 출석 수정 요청. 전부 MEMBER 이상이다 — GUEST 는 소모임에 참여할 수 없다.
 *
 * <p>요청은 그 기록의 명단에 있는 본인만, 목록·수락·거절은 그 소모임의 리더만 한다(서비스가 검사).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Authorize(@Condition(atLeast = UserRole.MEMBER))
public class ClubAttendanceController {

  private final ClubAttendanceService clubAttendanceService;

  @GetMapping("/clubs/{clubId}/activities/my-attendance")
  public ResponseEntity<ApiResponse<List<MyAttendanceResponse>, Void>> myAttendance(
      @AuthenticationPrincipal CustomUserDetails me, @PathVariable Long clubId) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_MY_ATTENDANCE_RETRIEVED",
            clubAttendanceService.myAttendance(clubId, me.getUserId())));
  }

  @PostMapping("/club-activities/{activityId}/fix-requests")
  public ResponseEntity<ApiResponse<Long, Void>> requestFix(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long activityId,
      @Valid @RequestBody ClubFixRequestCreateRequest req) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_FIX_REQUEST_CREATED",
            clubAttendanceService.requestFix(activityId, me.getUserId(), req.reason())));
  }

  @GetMapping("/clubs/{clubId}/fix-requests")
  public ResponseEntity<ApiResponse<List<ClubFixRequestResponse>, Void>> list(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @RequestParam(defaultValue = "PENDING") ClubFixRequestStatus status) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_FIX_REQUEST_LIST_RETRIEVED",
            clubAttendanceService.listForLeader(clubId, me.getUserId(), status)));
  }

  @PostMapping("/club-fix-requests/{requestId}/accept")
  public ResponseEntity<ApiResponse<Void, Void>> accept(
      @AuthenticationPrincipal CustomUserDetails me, @PathVariable Long requestId) {
    clubAttendanceService.accept(requestId, me.getUserId());
    return ResponseEntity.ok(ApiResponse.ok("CLUB_FIX_REQUEST_ACCEPTED"));
  }

  @PostMapping("/club-fix-requests/{requestId}/reject")
  public ResponseEntity<ApiResponse<Void, Void>> reject(
      @AuthenticationPrincipal CustomUserDetails me, @PathVariable Long requestId) {
    clubAttendanceService.reject(requestId, me.getUserId());
    return ResponseEntity.ok(ApiResponse.ok("CLUB_FIX_REQUEST_REJECTED"));
  }
}
