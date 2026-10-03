package inha.gdgoc.domain.club.club.controller;

import inha.gdgoc.domain.club.club.dto.request.AdminClubUpdateRequest;
import inha.gdgoc.domain.club.club.dto.request.ClubRejectRequest;
import inha.gdgoc.domain.club.club.service.ClubService;
import inha.gdgoc.domain.club.member.dto.request.ClubLeaderChangeRequest;
import inha.gdgoc.domain.club.member.service.ClubMemberService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 운영진(CORE 이상) — 개설 승인·반려, 소모임 상태·리더 교체. */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Authorize(@Condition(atLeast = UserRole.CORE))
public class ClubAdminController {

  private final ClubService clubService;
  private final ClubMemberService clubMemberService;

  @PostMapping("/clubs/{clubId}/approve")
  public ResponseEntity<ApiResponse<Void, Void>> approve(@PathVariable Long clubId) {
    clubService.approve(clubId);
    return ResponseEntity.ok(ApiResponse.ok("CLUB_APPROVED"));
  }

  @PostMapping("/clubs/{clubId}/reject")
  public ResponseEntity<ApiResponse<Void, Void>> reject(
      @PathVariable Long clubId, @Valid @RequestBody(required = false) ClubRejectRequest req) {
    clubService.reject(clubId, req == null ? null : req.reason());
    return ResponseEntity.ok(ApiResponse.ok("CLUB_REJECTED"));
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
