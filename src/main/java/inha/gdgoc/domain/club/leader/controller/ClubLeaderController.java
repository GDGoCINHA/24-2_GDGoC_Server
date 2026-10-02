package inha.gdgoc.domain.club.leader.controller;

import inha.gdgoc.domain.club.leader.dto.request.ClubOpenRequestCreateRequest;
import inha.gdgoc.domain.club.leader.dto.response.ClubOpenRequestResponse;
import inha.gdgoc.domain.club.leader.dto.response.MyLeaderGrantResponse;
import inha.gdgoc.domain.club.leader.service.ClubLeaderService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 내 리더 권한과 개설 신청. */
@RestController
@RequiredArgsConstructor
@Authorize(@Condition(atLeast = UserRole.MEMBER))
public class ClubLeaderController {

  private final ClubLeaderService clubLeaderService;

  @GetMapping("/api/v1/clubs/leader-grant/me")
  public ResponseEntity<ApiResponse<MyLeaderGrantResponse, Void>> myGrant(
      @AuthenticationPrincipal CustomUserDetails me) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_LEADER_GRANT_RETRIEVED",
            new MyLeaderGrantResponse(clubLeaderService.hasGrant(me.getUserId()))));
  }

  @PostMapping("/api/v1/club-open-requests")
  public ResponseEntity<ApiResponse<Long, Void>> requestOpen(
      @AuthenticationPrincipal CustomUserDetails me,
      @Valid @RequestBody ClubOpenRequestCreateRequest req) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_OPEN_REQUEST_CREATED", clubLeaderService.requestOpen(me.getUserId(), req)));
  }

  @GetMapping("/api/v1/club-open-requests/me")
  public ResponseEntity<ApiResponse<List<ClubOpenRequestResponse>, Void>> myRequests(
      @AuthenticationPrincipal CustomUserDetails me) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_OPEN_REQUEST_LIST_RETRIEVED", clubLeaderService.findMyRequests(me.getUserId())));
  }
}
