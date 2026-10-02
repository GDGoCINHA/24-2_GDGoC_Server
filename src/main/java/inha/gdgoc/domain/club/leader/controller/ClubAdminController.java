package inha.gdgoc.domain.club.leader.controller;

import inha.gdgoc.domain.club.club.dto.request.AdminClubUpdateRequest;
import inha.gdgoc.domain.club.club.service.ClubService;
import inha.gdgoc.domain.club.leader.dto.request.ClubLeaderGrantRequest;
import inha.gdgoc.domain.club.leader.dto.request.ClubOpenRequestRejectRequest;
import inha.gdgoc.domain.club.leader.dto.response.ClubLeaderGrantResponse;
import inha.gdgoc.domain.club.leader.dto.response.ClubOpenRequestResponse;
import inha.gdgoc.domain.club.leader.enums.ClubOpenRequestStatus;
import inha.gdgoc.domain.club.leader.service.ClubLeaderService;
import inha.gdgoc.domain.club.member.dto.request.ClubLeaderChangeRequest;
import inha.gdgoc.domain.club.member.service.ClubMemberService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 운영진(CORE 이상) — 개설 신청 심사, 리더 권한, 소모임 상태·리더 교체. */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Authorize(@Condition(atLeast = UserRole.CORE))
public class ClubAdminController {

  private final ClubLeaderService clubLeaderService;
  private final ClubService clubService;
  private final ClubMemberService clubMemberService;

  @GetMapping("/club-open-requests")
  public ResponseEntity<ApiResponse<List<ClubOpenRequestResponse>, Void>> openRequests(
      @RequestParam(required = false) ClubOpenRequestStatus status) {
    return ResponseEntity.ok(
        ApiResponse.ok("CLUB_OPEN_REQUEST_LIST_RETRIEVED", clubLeaderService.findRequests(status)));
  }

  /** 승인하면 리더 권한이 생긴다. 소모임은 신청자가 직접 연다. */
  @PostMapping("/club-open-requests/{requestId}/approve")
  public ResponseEntity<ApiResponse<Void, Void>> approve(
      @AuthenticationPrincipal CustomUserDetails me, @PathVariable Long requestId) {
    clubLeaderService.approve(requestId, me.getUserId());
    return ResponseEntity.ok(ApiResponse.ok("CLUB_OPEN_REQUEST_APPROVED"));
  }

  @PostMapping("/club-open-requests/{requestId}/reject")
  public ResponseEntity<ApiResponse<Void, Void>> reject(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long requestId,
      @Valid @RequestBody(required = false) ClubOpenRequestRejectRequest req) {
    clubLeaderService.reject(requestId, me.getUserId(), req == null ? null : req.reason());
    return ResponseEntity.ok(ApiResponse.ok("CLUB_OPEN_REQUEST_REJECTED"));
  }

  @GetMapping("/club-leader-grants")
  public ResponseEntity<ApiResponse<List<ClubLeaderGrantResponse>, Void>> grants() {
    return ResponseEntity.ok(
        ApiResponse.ok("CLUB_LEADER_GRANT_LIST_RETRIEVED", clubLeaderService.findGrants()));
  }

  @PostMapping("/club-leader-grants")
  public ResponseEntity<ApiResponse<Void, Void>> grant(
      @AuthenticationPrincipal CustomUserDetails me,
      @Valid @RequestBody ClubLeaderGrantRequest req) {
    clubLeaderService.grant(req.userId(), me.getUserId());
    return ResponseEntity.ok(ApiResponse.ok("CLUB_LEADER_GRANTED"));
  }

  @DeleteMapping("/club-leader-grants/{userId}")
  public ResponseEntity<ApiResponse<Void, Void>> revoke(@PathVariable Long userId) {
    clubLeaderService.revoke(userId);
    return ResponseEntity.ok(ApiResponse.ok("CLUB_LEADER_REVOKED"));
  }

  @PatchMapping("/clubs/{clubId}")
  public ResponseEntity<ApiResponse<Void, Void>> updateClub(
      @PathVariable Long clubId, @Valid @RequestBody AdminClubUpdateRequest req) {
    clubService.updateByStaff(clubId, req);
    return ResponseEntity.ok(ApiResponse.ok("CLUB_UPDATED"));
  }

  @PostMapping("/clubs/{clubId}/leader")
  public ResponseEntity<ApiResponse<Void, Void>> changeLeader(
      @PathVariable Long clubId, @Valid @RequestBody ClubLeaderChangeRequest req) {
    clubMemberService.changeLeaderByStaff(clubId, req.userId());
    return ResponseEntity.ok(ApiResponse.ok("CLUB_LEADER_CHANGED"));
  }
}
