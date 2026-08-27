package inha.gdgoc.domain.eventapplication.controller;

import static inha.gdgoc.domain.eventapplication.controller.message.EventApplicationMessage.*;

import inha.gdgoc.domain.eventapplication.dto.request.AttendanceUpdateRequest;
import inha.gdgoc.domain.eventapplication.dto.request.ProxyApplicationRequest;
import inha.gdgoc.domain.eventapplication.dto.response.ApplicantResponse;
import inha.gdgoc.domain.eventapplication.dto.response.CheckinTokenResponse;
import inha.gdgoc.domain.eventapplication.enums.ApplicationStatus;
import inha.gdgoc.domain.eventapplication.service.EventApplicantAdminService;
import inha.gdgoc.domain.eventapplication.service.EventCheckinService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.dto.response.PageMeta;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 운영진의 신청자 확인·참석 처리. */
@RestController
@RequestMapping("/api/v1/admin/events/{eventBoardId}/applications")
@RequiredArgsConstructor
@Validated
@Authorize(@Condition(atLeast = UserRole.CORE))
public class AdminEventApplicantController {

  private final EventApplicantAdminService eventApplicantAdminService;
  private final EventCheckinService eventCheckinService;

  @GetMapping
  public ResponseEntity<ApiResponse<Page<ApplicantResponse>, PageMeta>> listApplicants(
      @PathVariable Long eventBoardId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size,
      @RequestParam(required = false) ApplicationStatus status) {
    Page<ApplicantResponse> result =
        eventApplicantAdminService.listApplicants(
            eventBoardId, status, PageRequest.of(page, size, Sort.by("appliedAt").ascending()));
    return ResponseEntity.ok(
        ApiResponse.ok(APPLICANTS_RETRIEVED, result, PageMeta.of(result)));
  }

  /** 파일 응답이라 ApiResponse 로 감싸지 않는다. */
  @GetMapping("/export")
  public ResponseEntity<byte[]> exportCsv(
      @PathVariable Long eventBoardId, @RequestParam(required = false) ApplicationStatus status) {
    byte[] body = eventApplicantAdminService.exportCsv(eventBoardId, status);
    String fileName =
        URLEncoder.encode(eventApplicantAdminService.csvFileName(eventBoardId), StandardCharsets.UTF_8)
            .replace("+", "%20");

    return ResponseEntity.ok()
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
        .body(body);
  }

  /** QR 화면이 만료 전에 다시 불러 코드를 새로 그린다. */
  @GetMapping("/checkin-token")
  public ResponseEntity<ApiResponse<CheckinTokenResponse, Void>> issueCheckinToken(
      @PathVariable Long eventBoardId) {
    return ResponseEntity.ok(
        ApiResponse.ok(CHECKIN_TOKEN_ISSUED, eventCheckinService.issueToken(eventBoardId)));
  }

  @PatchMapping("/{applicationId}/attendance")
  public ResponseEntity<ApiResponse<Void, Void>> updateAttendance(
      @PathVariable Long eventBoardId,
      @PathVariable Long applicationId,
      @Valid @RequestBody AttendanceUpdateRequest req) {
    eventApplicantAdminService.updateAttendance(eventBoardId, applicationId, req);
    return ResponseEntity.ok(ApiResponse.ok(ATTENDANCE_UPDATED));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<Long, Void>> registerProxy(
      @PathVariable Long eventBoardId, @Valid @RequestBody ProxyApplicationRequest req) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            PROXY_REGISTERED, eventApplicantAdminService.registerProxy(eventBoardId, req)));
  }
}
