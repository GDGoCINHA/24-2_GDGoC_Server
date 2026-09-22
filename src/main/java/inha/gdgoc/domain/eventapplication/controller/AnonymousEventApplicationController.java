package inha.gdgoc.domain.eventapplication.controller;

import static inha.gdgoc.domain.eventapplication.controller.message.EventApplicationMessage.*;

import inha.gdgoc.domain.eventapplication.dto.request.AnonymousApplicationRequest;
import inha.gdgoc.domain.eventapplication.dto.request.AnonymousCheckinRequest;
import inha.gdgoc.domain.eventapplication.dto.response.CheckinResponse;
import inha.gdgoc.domain.eventapplication.dto.response.EventFormPublicResponse;
import inha.gdgoc.domain.eventapplication.service.EventApplicationService;
import inha.gdgoc.domain.eventapplication.service.EventCheckinService;
import inha.gdgoc.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인하지 않은 사람의 행사 신청.
 *
 * <p>{@link EventApplicationController} 는 클래스 단위로 로그인을 요구하므로 따로 둔다. 여기 경로는 SecurityConfig 에서 하나씩 열려
 * 있다. 로그인 없이 받을지는 폼마다 정하고({@code allowAnonymous}), 판정은 서비스가 한다.
 */
@RestController
@RequestMapping("/api/v1/board/events/{eventBoardId}/anonymous")
@RequiredArgsConstructor
@Validated
public class AnonymousEventApplicationController {

  private final EventApplicationService eventApplicationService;
  private final EventCheckinService eventCheckinService;

  /** 로그인 없이 받지 않는 폼도 내려준다. 화면이 로그인 안내를 띄울 수 있게 canApply 와 사유가 함께 온다. */
  @GetMapping("/form")
  public ResponseEntity<ApiResponse<EventFormPublicResponse, Void>> getForm(
      @PathVariable Long eventBoardId) {
    return ResponseEntity.ok(
        ApiResponse.ok(FORM_RETRIEVED, eventApplicationService.getForm(eventBoardId, null, null)));
  }

  @PostMapping("/applications")
  public ResponseEntity<ApiResponse<Void, Void>> apply(
      @PathVariable Long eventBoardId, @Valid @RequestBody AnonymousApplicationRequest req) {
    eventApplicationService.applyAnonymously(eventBoardId, req);
    return ResponseEntity.ok(ApiResponse.ok(APPLICATION_SUBMITTED));
  }

  @PostMapping("/checkin")
  public ResponseEntity<ApiResponse<CheckinResponse, Void>> checkIn(
      @PathVariable Long eventBoardId, @Valid @RequestBody AnonymousCheckinRequest req) {
    CheckinResponse result =
        eventCheckinService.checkInAnonymously(
            eventBoardId, req.token(), req.studentId(), req.name());
    return ResponseEntity.ok(
        ApiResponse.ok(result.alreadyCheckedIn() ? CHECKIN_ALREADY : CHECKIN_DONE, result));
  }
}
