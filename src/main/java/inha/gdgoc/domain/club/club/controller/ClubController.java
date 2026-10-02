package inha.gdgoc.domain.club.club.controller;

import inha.gdgoc.domain.club.club.dto.request.ClubCreateRequest;
import inha.gdgoc.domain.club.club.dto.request.ClubUpdateRequest;
import inha.gdgoc.domain.club.club.dto.response.ClubDetailResponse;
import inha.gdgoc.domain.club.club.dto.response.ClubSummaryResponse;
import inha.gdgoc.domain.club.club.dto.response.MyClubResponse;
import inha.gdgoc.domain.club.club.enums.ClubCategory;
import inha.gdgoc.domain.club.club.enums.ClubRecruitStatus;
import inha.gdgoc.domain.club.club.service.ClubService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.dto.response.PageMeta;
import inha.gdgoc.global.security.AccessGuard;
import inha.gdgoc.global.security.AccessGuard.AccessCondition;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

/** 소모임 게시판. 목록·상세는 누구나, 개설·수정은 MEMBER 이상 + 데이터 권한(리더 권한·리더). */
@RestController
@RequestMapping("/api/v1/clubs")
@RequiredArgsConstructor
public class ClubController {

  private final ClubService clubService;
  private final AccessGuard accessGuard;

  @GetMapping
  public ResponseEntity<ApiResponse<List<ClubSummaryResponse>, PageMeta>> list(
      @AuthenticationPrincipal CustomUserDetails me,
      @RequestParam(required = false) ClubCategory category,
      @RequestParam(required = false) ClubRecruitStatus recruitStatus,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Page<ClubSummaryResponse> result =
        clubService.search(
            category,
            recruitStatus,
            keyword,
            isStaff(me),
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
    return ResponseEntity.ok(
        ApiResponse.ok("CLUB_LIST_RETRIEVED", result.getContent(), PageMeta.of(result)));
  }

  /** 내가 신청했거나 참여 중인 소모임. */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<List<MyClubResponse>, Void>> mine(
      @AuthenticationPrincipal CustomUserDetails me) {
    return ResponseEntity.ok(
        ApiResponse.ok("MY_CLUB_LIST_RETRIEVED", clubService.findMine(me.getUserId())));
  }

  @GetMapping("/{clubId}")
  public ResponseEntity<ApiResponse<ClubDetailResponse, Void>> detail(
      @AuthenticationPrincipal CustomUserDetails me, @PathVariable Long clubId) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_RETRIEVED", clubService.getDetail(clubId, me == null ? null : me.getUserId(), isStaff(me))));
  }

  @Authorize(@Condition(atLeast = UserRole.MEMBER))
  @PostMapping
  public ResponseEntity<ApiResponse<Long, Void>> create(
      @AuthenticationPrincipal CustomUserDetails me, @Valid @RequestBody ClubCreateRequest req) {
    return ResponseEntity.ok(ApiResponse.ok("CLUB_CREATED", clubService.create(me.getUserId(), req)));
  }

  @Authorize(@Condition(atLeast = UserRole.MEMBER))
  @PatchMapping("/{clubId}")
  public ResponseEntity<ApiResponse<Void, Void>> update(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long clubId,
      @Valid @RequestBody ClubUpdateRequest req) {
    clubService.update(clubId, me.getUserId(), req);
    return ResponseEntity.ok(ApiResponse.ok("CLUB_UPDATED"));
  }

  /** 목록·상세는 비로그인도 부른다(SecurityConfig). 그때 {@code me} 는 null 이다. */
  private boolean isStaff(CustomUserDetails me) {
    return me != null && accessGuard.check(me, AccessCondition.atLeast(UserRole.CORE));
  }
}
