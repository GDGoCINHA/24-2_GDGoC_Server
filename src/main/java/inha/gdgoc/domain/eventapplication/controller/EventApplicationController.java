package inha.gdgoc.domain.eventapplication.controller;

import static inha.gdgoc.domain.eventapplication.controller.message.EventApplicationMessage.*;

import inha.gdgoc.domain.eventapplication.dto.request.CheckinRequest;
import inha.gdgoc.domain.eventapplication.dto.request.EventApplicationSubmitRequest;
import inha.gdgoc.domain.eventapplication.dto.response.CheckinResponse;
import inha.gdgoc.domain.eventapplication.dto.response.EventFormPublicResponse;
import inha.gdgoc.domain.eventapplication.service.EventApplicationService;
import inha.gdgoc.domain.eventapplication.service.EventCheckinService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 부원의 행사 신청.
 *
 * <p>신청 자격은 행사마다 다르므로({@code min_role}) 여기서는 로그인만 요구하고 실제 자격은 서비스가 판정한다. 외부 공개 행사는 {@code GUEST} 까지
 * 신청할 수 있다.
 */
@RestController
@RequestMapping("/api/v1/board/events/{eventBoardId}")
@RequiredArgsConstructor
@Validated
@Authorize(@Condition(atLeast = UserRole.GUEST))
public class EventApplicationController {

  private final EventApplicationService eventApplicationService;
  private final EventCheckinService eventCheckinService;

  @GetMapping("/form")
  public ResponseEntity<ApiResponse<EventFormPublicResponse, Void>> getForm(
      @AuthenticationPrincipal CustomUserDetails me, @PathVariable Long eventBoardId) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            FORM_RETRIEVED,
            eventApplicationService.getForm(eventBoardId, me.getUserId(), me.getRole())));
  }

  @PostMapping("/applications")
  public ResponseEntity<ApiResponse<Void, Void>> apply(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long eventBoardId,
      @Valid @RequestBody EventApplicationSubmitRequest req) {
    eventApplicationService.apply(eventBoardId, req, me.getUserId(), me.getRole());
    return ResponseEntity.ok(ApiResponse.ok(APPLICATION_SUBMITTED));
  }

  /** QR 을 찍으면 열리는 화면이 부른다. */
  @PostMapping("/checkin")
  public ResponseEntity<ApiResponse<CheckinResponse, Void>> checkIn(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long eventBoardId,
      @Valid @RequestBody CheckinRequest req) {
    CheckinResponse result =
        eventCheckinService.checkIn(eventBoardId, req.token(), me.getUserId());
    return ResponseEntity.ok(
        ApiResponse.ok(result.alreadyCheckedIn() ? CHECKIN_ALREADY : CHECKIN_DONE, result));
  }

  @DeleteMapping("/applications/me")
  public ResponseEntity<ApiResponse<Void, Void>> cancel(
      @AuthenticationPrincipal CustomUserDetails me, @PathVariable Long eventBoardId) {
    eventApplicationService.cancel(eventBoardId, me.getUserId());
    return ResponseEntity.ok(ApiResponse.ok(APPLICATION_CANCELED));
  }
}
