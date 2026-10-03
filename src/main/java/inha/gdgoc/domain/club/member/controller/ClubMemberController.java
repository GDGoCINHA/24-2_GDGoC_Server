package inha.gdgoc.domain.club.member.controller;

import inha.gdgoc.domain.club.member.dto.request.ClubApplyRequest;
import inha.gdgoc.domain.club.member.dto.request.ClubLeaderChangeRequest;
import inha.gdgoc.domain.club.member.dto.response.ClubMemberResponse;
import inha.gdgoc.domain.club.member.service.ClubMemberService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 참여·탈퇴·승인·강퇴·리더 교체. GUEST 는 참여할 수 없다. */
@RestController
@RequestMapping("/api/v1/clubs/{clubId}")
@RequiredArgsConstructor
@Authorize(@Condition(atLeast = UserRole.MEMBER))
public class ClubMemberController {

  private final ClubMemberService clubMemberService;
  private final AccessGuard accessGuard;

  @PostMapping("/members")
  public ResponseEntity<ApiResponse<Void, Void>> apply(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @Valid @RequestBody(required = false) ClubApplyRequest req) {
    clubMemberService.apply(clubId, me.getUserId(), req == null ? null : req.message());
    return ResponseEntity.ok(ApiResponse.ok("CLUB_APPLIED"));
  }

  /** 신청 취소(PENDING) 또는 탈퇴(ACTIVE). */
  @DeleteMapping("/members/me")
  public ResponseEntity<ApiResponse<Void, Void>> leave(
      @AuthenticationPrincipal CustomUserDetails me, @PathVariable Long clubId) {
    clubMemberService.leave(clubId, me.getUserId());
    return ResponseEntity.ok(ApiResponse.ok("CLUB_LEFT"));
  }

  @GetMapping("/members")
  public ResponseEntity<ApiResponse<List<ClubMemberResponse>, Void>> members(
      @AuthenticationPrincipal CustomUserDetails me, @PathVariable Long clubId) {
    boolean staff = accessGuard.check(me, AccessCondition.atLeast(UserRole.CORE));
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_MEMBER_LIST_RETRIEVED",
            clubMemberService.findActive(clubId, me.getUserId(), staff)));
  }

  @GetMapping("/members/pending")
  public ResponseEntity<ApiResponse<List<ClubMemberResponse>, Void>> pending(
      @AuthenticationPrincipal CustomUserDetails me, @PathVariable Long clubId) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_APPLICANT_LIST_RETRIEVED", clubMemberService.findPending(clubId, me.getUserId())));
  }

  @PostMapping("/members/{memberId}/approve")
  public ResponseEntity<ApiResponse<Void, Void>> approve(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @PathVariable Long memberId) {
    clubMemberService.approve(clubId, memberId, me.getUserId());
    return ResponseEntity.ok(ApiResponse.ok("CLUB_MEMBER_APPROVED"));
  }

  @PostMapping("/members/{memberId}/reject")
  public ResponseEntity<ApiResponse<Void, Void>> reject(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @PathVariable Long memberId) {
    clubMemberService.reject(clubId, memberId, me.getUserId());
    return ResponseEntity.ok(ApiResponse.ok("CLUB_MEMBER_REJECTED"));
  }

  @PostMapping("/members/{memberId}/kick")
  public ResponseEntity<ApiResponse<Void, Void>> kick(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @PathVariable Long memberId) {
    clubMemberService.kick(clubId, memberId, me.getUserId());
    return ResponseEntity.ok(ApiResponse.ok("CLUB_MEMBER_KICKED"));
  }

  @PostMapping("/leader")
  public ResponseEntity<ApiResponse<Void, Void>> changeLeader(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @Valid @RequestBody ClubLeaderChangeRequest req) {
    clubMemberService.changeLeader(clubId, me.getUserId(), req.userId());
    return ResponseEntity.ok(ApiResponse.ok("CLUB_LEADER_CHANGED"));
  }
}
