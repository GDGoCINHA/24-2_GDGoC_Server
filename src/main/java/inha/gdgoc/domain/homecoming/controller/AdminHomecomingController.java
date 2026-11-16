package inha.gdgoc.domain.homecoming.controller;

import static inha.gdgoc.domain.homecoming.controller.message.HomecomingMessage.*;

import inha.gdgoc.domain.homecoming.dto.request.HomecomingCancelRequest;
import inha.gdgoc.domain.homecoming.dto.request.HomecomingUpdateRequest;
import inha.gdgoc.domain.homecoming.dto.response.HomecomingApplicationResponse;
import inha.gdgoc.domain.homecoming.dto.response.HomecomingConsentEntryResponse;
import inha.gdgoc.domain.homecoming.dto.response.HomecomingSummaryResponse;
import inha.gdgoc.domain.homecoming.enums.AttendType;
import inha.gdgoc.domain.homecoming.enums.ConsentPurpose;
import inha.gdgoc.domain.homecoming.enums.ConsentState;
import inha.gdgoc.domain.homecoming.enums.HomecomingStatus;
import inha.gdgoc.domain.homecoming.service.HomecomingAdminService;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
 * 홈커밍데이 신청 관리. 명세상 회장·HR 담당자만 전체 내역을 보고 고친다.
 *
 * <p>회장 = ORGANIZER 이상, HR 담당자 = HR 팀 CORE 이상으로 본다. 다른 팀 코어·리드는 막힌다 — 전화번호가 담긴 명단이라서다.
 * HomecomingAdminAuthorizeTest 가 지킨다.
 */
@RestController
@RequestMapping("/api/v1/admin/homecoming")
@RequiredArgsConstructor
@Authorize({
  @Condition(atLeast = UserRole.ORGANIZER),
  @Condition(atLeast = UserRole.CORE, teams = TeamType.HR)
})
public class AdminHomecomingController {

  private final HomecomingAdminService adminService;

  @GetMapping("/applications")
  public ResponseEntity<ApiResponse<List<HomecomingApplicationResponse>, Void>> list(
      @RequestParam(required = false) HomecomingStatus status,
      @RequestParam(required = false) AttendType attendType) {
    return ResponseEntity.ok(ApiResponse.ok(APPLICATIONS_RETRIEVED, adminService.list(status, attendType)));
  }

  @GetMapping("/applications/{id:[0-9]+}")
  public ResponseEntity<ApiResponse<HomecomingApplicationResponse, Void>> get(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.ok(APPLICATION_RETRIEVED, adminService.get(id)));
  }

  @PatchMapping("/applications/{id:[0-9]+}")
  public ResponseEntity<ApiResponse<HomecomingApplicationResponse, Void>> update(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long id,
      @Valid @RequestBody HomecomingUpdateRequest req) {
    return ResponseEntity.ok(
        ApiResponse.ok(APPLICATION_UPDATED, adminService.update(id, req, me.getUserId())));
  }

  @PostMapping("/applications/{id:[0-9]+}/cancel")
  public ResponseEntity<ApiResponse<Void, Void>> cancel(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long id,
      @Valid @RequestBody(required = false) HomecomingCancelRequest req) {
    adminService.cancel(id, req == null ? null : req.reason(), me.getUserId());
    return ResponseEntity.ok(ApiResponse.ok(APPLICATION_CANCELED));
  }

  @GetMapping("/summary")
  public ResponseEntity<ApiResponse<HomecomingSummaryResponse, Void>> summary() {
    return ResponseEntity.ok(ApiResponse.ok(SUMMARY_RETRIEVED, adminService.summary()));
  }

  /** 파일 응답이라 ApiResponse 로 감싸지 않는다. */
  @GetMapping("/export/nametags")
  public ResponseEntity<byte[]> exportNametags() {
    return csv(adminService.exportNametags(), "홈커밍데이_명찰.csv");
  }

  @GetMapping("/export/applications")
  public ResponseEntity<byte[]> exportApplications() {
    return csv(adminService.exportApplications(), "홈커밍데이_신청명단.csv");
  }

  @GetMapping("/consents")
  public ResponseEntity<ApiResponse<List<HomecomingConsentEntryResponse>, Void>> consents(
      @RequestParam ConsentPurpose purpose, @RequestParam(required = false) ConsentState state) {
    return ResponseEntity.ok(ApiResponse.ok(CONSENTS_RETRIEVED, adminService.consents(purpose, state)));
  }

  @PostMapping("/applications/{id:[0-9]+}/consents/{purpose}/withdraw")
  public ResponseEntity<ApiResponse<Void, Void>> withdrawConsent(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long id,
      @PathVariable ConsentPurpose purpose) {
    adminService.withdrawConsent(id, purpose, me.getUserId());
    return ResponseEntity.ok(ApiResponse.ok(CONSENT_WITHDRAWN));
  }

  private static ResponseEntity<byte[]> csv(byte[] body, String fileName) {
    String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
    return ResponseEntity.ok()
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
        .body(body);
  }
}
