package inha.gdgoc.domain.club.activity.controller;

import inha.gdgoc.domain.club.activity.dto.request.ClubActivitySubmitRequest;
import inha.gdgoc.domain.club.activity.dto.response.ClubActivityDetailResponse;
import inha.gdgoc.domain.club.activity.dto.response.ClubActivityRosterResponse;
import inha.gdgoc.domain.club.activity.service.ClubActivityService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.security.AccessGuard;
import inha.gdgoc.global.security.AccessGuard.AccessCondition;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 활동 기록(인증). 전부 MEMBER 이상이다 — GUEST 는 소모임에 참여할 수 없다.
 *
 * <p>작성·수정·명단은 그 소모임의 리더만, 상세는 부원 누구나 본다(출석 명단은 서비스가 가린다).
 */
@RestController
@RequestMapping("/api/v1/clubs/{clubId}/activities")
@RequiredArgsConstructor
@Authorize(@Condition(atLeast = UserRole.MEMBER))
public class ClubActivityController {

  private final ClubActivityService clubActivityService;
  private final AccessGuard accessGuard;

  @GetMapping("/roster")
  public ResponseEntity<ApiResponse<ClubActivityRosterResponse, Void>> roster(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) Long scheduleId) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_ACTIVITY_ROSTER_RETRIEVED",
            clubActivityService.roster(clubId, me.getUserId(), date, scheduleId)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<Long, Void>> create(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @Valid @RequestBody ClubActivitySubmitRequest req) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_ACTIVITY_CREATED", clubActivityService.create(clubId, me.getUserId(), req)));
  }

  @GetMapping("/{activityId}")
  public ResponseEntity<ApiResponse<ClubActivityDetailResponse, Void>> detail(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @PathVariable Long activityId) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_ACTIVITY_RETRIEVED",
            clubActivityService.detail(clubId, activityId, me.getUserId(), isStaff(me))));
  }

  @PatchMapping("/{activityId}")
  public ResponseEntity<ApiResponse<Void, Void>> update(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @PathVariable Long activityId,
      @Valid @RequestBody ClubActivitySubmitRequest req) {
    clubActivityService.update(clubId, activityId, me.getUserId(), req);
    return ResponseEntity.ok(ApiResponse.ok("CLUB_ACTIVITY_UPDATED"));
  }

  private boolean isStaff(CustomUserDetails me) {
    return accessGuard.check(me, AccessCondition.atLeast(UserRole.CORE));
  }
}
