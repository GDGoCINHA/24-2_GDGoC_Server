package inha.gdgoc.domain.club.export.controller;

import inha.gdgoc.domain.club.export.dto.AdminClubRowResponse;
import inha.gdgoc.domain.club.export.service.ClubAdminStatusService;
import inha.gdgoc.domain.club.export.service.ClubStatusCsvWriter;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 운영진(CORE 이상)의 팀별 현황 표와 CSV 내보내기. */
@RestController
@RequestMapping("/api/v1/admin/clubs")
@RequiredArgsConstructor
@Authorize(@Condition(atLeast = UserRole.CORE))
public class ClubAdminStatusController {

  private final ClubAdminStatusService clubAdminStatusService;
  private final ClubStatusCsvWriter clubStatusCsvWriter;

  /** {@code warning}: 비우면 전체, {@code ANY} 면 경고 있는 팀, 경고 이름이면 그 경고가 있는 팀. */
  @GetMapping
  public ResponseEntity<ApiResponse<List<AdminClubRowResponse>, Void>> list(
      @RequestParam(required = false) Long termId,
      @RequestParam(required = false) String warning) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            "CLUB_ADMIN_STATUS_RETRIEVED", clubAdminStatusService.findRows(termId, warning)));
  }

  /** 파일 응답이라 ApiResponse 로 감싸지 않는다. 표와 같은 행을 그대로 쓴다(필터 없음). */
  @GetMapping("/export")
  public ResponseEntity<byte[]> export(@RequestParam(required = false) Long termId) {
    byte[] body = clubStatusCsvWriter.write(clubAdminStatusService.findRows(termId, null));
    String fileName =
        URLEncoder.encode(
                "소모임현황-" + LocalDate.now(ZoneId.of("Asia/Seoul")) + ".csv", StandardCharsets.UTF_8)
            .replace("+", "%20");
    return ResponseEntity.ok()
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
        .body(body);
  }
}
