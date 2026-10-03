package inha.gdgoc.domain.club.schedule.controller;

import inha.gdgoc.domain.club.schedule.dto.request.ClubCheckinRequest;
import inha.gdgoc.domain.club.schedule.dto.request.ClubRsvpRequest;
import inha.gdgoc.domain.club.schedule.dto.request.ClubScheduleRequest;
import inha.gdgoc.domain.club.schedule.dto.response.ClubCheckinResponse;
import inha.gdgoc.domain.club.schedule.dto.response.ClubCheckinTokenResponse;
import inha.gdgoc.domain.club.schedule.dto.response.ClubScheduleResponse;
import inha.gdgoc.domain.club.schedule.service.ClubCheckinService;
import inha.gdgoc.domain.club.schedule.service.ClubScheduleService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.security.AccessGuard;
import inha.gdgoc.global.security.AccessGuard.AccessCondition;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 일정·참석 응답·QR 체크인. 전부 MEMBER 이상이다 — GUEST 는 소모임에 참여할 수 없다.
 *
 * <p>목록은 부원 누구나, 등록·수정·삭제·QR 토큰은 그 소모임의 리더만, 참석 응답·체크인은 그 소모임의 멤버만 한다(서비스가 검사).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Authorize(@Condition(atLeast = UserRole.MEMBER))
public class ClubScheduleController {

  private final ClubScheduleService clubScheduleService;
  private final ClubCheckinService clubCheckinService;
  private final AccessGuard accessGuard;

  /** {@code scope=past} 면 지난 일정(최근 순), 그 밖에는 다가오는 일정(가까운 순). */
  @GetMapping("/clubs/{clubId}/schedules")
  public ResponseEntity<ApiResponse<List<ClubScheduleResponse>, Void>> list(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @RequestParam(defaultValue = "upcoming") String scope) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_SCHEDULE_LIST_RETRIEVED",
            clubScheduleService.list(
                clubId, me.getUserId(), isStaff(me), "past".equalsIgnoreCase(scope))));
  }

  @PostMapping("/clubs/{clubId}/schedules")
  public ResponseEntity<ApiResponse<Long, Void>> create(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @Valid @RequestBody ClubScheduleRequest req) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_SCHEDULE_CREATED", clubScheduleService.create(clubId, me.getUserId(), req)));
  }

  @PatchMapping("/clubs/{clubId}/schedules/{scheduleId}")
  public ResponseEntity<ApiResponse<Void, Void>> update(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @PathVariable Long scheduleId,
      @Valid @RequestBody ClubScheduleRequest req) {
    clubScheduleService.update(clubId, scheduleId, me.getUserId(), req);
    return ResponseEntity.ok(ApiResponse.ok("CLUB_SCHEDULE_UPDATED"));
  }

  @DeleteMapping("/clubs/{clubId}/schedules/{scheduleId}")
  public ResponseEntity<ApiResponse<Void, Void>> delete(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @PathVariable Long scheduleId) {
    clubScheduleService.delete(clubId, scheduleId, me.getUserId());
    return ResponseEntity.ok(ApiResponse.ok("CLUB_SCHEDULE_DELETED"));
  }

  @PutMapping("/club-schedules/{scheduleId}/rsvp")
  public ResponseEntity<ApiResponse<Void, Void>> rsvp(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long scheduleId,
      @Valid @RequestBody ClubRsvpRequest req) {
    clubScheduleService.respond(scheduleId, me.getUserId(), req.response());
    return ResponseEntity.ok(ApiResponse.ok("CLUB_SCHEDULE_RSVP_SAVED"));
  }

  @GetMapping("/club-schedules/{scheduleId}/checkin-token")
  public ResponseEntity<ApiResponse<ClubCheckinTokenResponse, Void>> checkinToken(
      @AuthenticationPrincipal CustomUserDetails me, @PathVariable Long scheduleId) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_CHECKIN_TOKEN_ISSUED",
            clubScheduleService.issueCheckinToken(scheduleId, me.getUserId())));
  }

  @PostMapping("/club-checkin")
  public ResponseEntity<ApiResponse<ClubCheckinResponse, Void>> checkIn(
      @AuthenticationPrincipal CustomUserDetails me, @Valid @RequestBody ClubCheckinRequest req) {
    return ResponseEntity.ok(
        ApiResponse.ok("CLUB_CHECKED_IN", clubCheckinService.checkIn(req.token(), me.getUserId())));
  }

  private boolean isStaff(CustomUserDetails me) {
    return accessGuard.check(me, AccessCondition.atLeast(UserRole.CORE));
  }
}
