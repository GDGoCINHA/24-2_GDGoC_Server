package inha.gdgoc.domain.club.term.controller;

import inha.gdgoc.domain.club.term.dto.ClubTermCreateRequest;
import inha.gdgoc.domain.club.term.dto.ClubTermResponse;
import inha.gdgoc.domain.club.term.dto.ClubTermUpdateRequest;
import inha.gdgoc.domain.club.term.service.ClubTermService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 기수 — 목록은 부원, 생성·수정은 운영진(CORE 이상). */
@RestController
@RequiredArgsConstructor
public class ClubTermController {

  private final ClubTermService clubTermService;

  @Authorize(@Condition(atLeast = UserRole.MEMBER))
  @GetMapping("/api/v1/club-terms")
  public ResponseEntity<ApiResponse<List<ClubTermResponse>, Void>> list() {
    return ResponseEntity.ok(ApiResponse.ok("CLUB_TERM_LIST_RETRIEVED", clubTermService.findAll()));
  }

  @Authorize(@Condition(atLeast = UserRole.CORE))
  @PostMapping("/api/v1/admin/club-terms")
  public ResponseEntity<ApiResponse<ClubTermResponse, Void>> create(
      @Valid @RequestBody ClubTermCreateRequest req) {
    return ResponseEntity.ok(ApiResponse.ok("CLUB_TERM_CREATED", clubTermService.create(req)));
  }

  @Authorize(@Condition(atLeast = UserRole.CORE))
  @PatchMapping("/api/v1/admin/club-terms/{termId}")
  public ResponseEntity<ApiResponse<ClubTermResponse, Void>> update(
      @PathVariable Long termId, @Valid @RequestBody ClubTermUpdateRequest req) {
    return ResponseEntity.ok(
        ApiResponse.ok("CLUB_TERM_UPDATED", clubTermService.update(termId, req)));
  }
}
