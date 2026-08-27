package inha.gdgoc.domain.eventapplication.controller;

import static inha.gdgoc.domain.eventapplication.controller.message.EventApplicationMessage.*;

import inha.gdgoc.domain.eventapplication.dto.request.EventFormSaveRequest;
import inha.gdgoc.domain.eventapplication.dto.request.QuestionOrderRequest;
import inha.gdgoc.domain.eventapplication.dto.request.QuestionSaveRequest;
import inha.gdgoc.domain.eventapplication.dto.response.EventFormResponse;
import inha.gdgoc.domain.eventapplication.service.EventFormAdminService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 운영진의 신청 폼 관리. 행사 게시글 하나에 폼 하나가 붙는다. */
@RestController
@RequestMapping("/api/v1/admin/events/{eventBoardId}/form")
@RequiredArgsConstructor
@Validated
@Authorize(@Condition(atLeast = UserRole.CORE))
public class AdminEventFormController {

  private final EventFormAdminService eventFormAdminService;

  @GetMapping
  public ResponseEntity<ApiResponse<EventFormResponse, Void>> getForm(
      @PathVariable Long eventBoardId) {
    return ResponseEntity.ok(
        ApiResponse.ok(FORM_RETRIEVED, eventFormAdminService.getForm(eventBoardId)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<Long, Void>> createForm(
      @PathVariable Long eventBoardId, @Valid @RequestBody EventFormSaveRequest req) {
    return ResponseEntity.ok(
        ApiResponse.ok(FORM_CREATED, eventFormAdminService.createForm(eventBoardId, req)));
  }

  @PutMapping
  public ResponseEntity<ApiResponse<Void, Void>> updateForm(
      @PathVariable Long eventBoardId, @Valid @RequestBody EventFormSaveRequest req) {
    eventFormAdminService.updateForm(eventBoardId, req);
    return ResponseEntity.ok(ApiResponse.ok(FORM_UPDATED));
  }

  /** 부원에게 공개한다. 이 요청 전까지 행사 상세에는 신청 영역이 아예 그려지지 않는다. */
  @PostMapping("/publish")
  public ResponseEntity<ApiResponse<Void, Void>> publishForm(@PathVariable Long eventBoardId) {
    eventFormAdminService.publishForm(eventBoardId);
    return ResponseEntity.ok(ApiResponse.ok(FORM_PUBLISHED));
  }

  @DeleteMapping
  public ResponseEntity<ApiResponse<Void, Void>> deleteForm(@PathVariable Long eventBoardId) {
    eventFormAdminService.deleteForm(eventBoardId);
    return ResponseEntity.ok(ApiResponse.ok(FORM_DELETED));
  }

  @PostMapping("/questions")
  public ResponseEntity<ApiResponse<Long, Void>> addQuestion(
      @PathVariable Long eventBoardId, @Valid @RequestBody QuestionSaveRequest req) {
    return ResponseEntity.ok(
        ApiResponse.ok(QUESTION_CREATED, eventFormAdminService.addQuestion(eventBoardId, req)));
  }

  @PutMapping("/questions/order")
  public ResponseEntity<ApiResponse<Void, Void>> reorderQuestions(
      @PathVariable Long eventBoardId, @Valid @RequestBody QuestionOrderRequest req) {
    eventFormAdminService.reorderQuestions(eventBoardId, req);
    return ResponseEntity.ok(ApiResponse.ok(QUESTION_ORDER_UPDATED));
  }

  @PutMapping("/questions/{questionId}")
  public ResponseEntity<ApiResponse<Void, Void>> updateQuestion(
      @PathVariable Long eventBoardId,
      @PathVariable Long questionId,
      @Valid @RequestBody QuestionSaveRequest req) {
    eventFormAdminService.updateQuestion(eventBoardId, questionId, req);
    return ResponseEntity.ok(ApiResponse.ok(QUESTION_UPDATED));
  }

  @DeleteMapping("/questions/{questionId}")
  public ResponseEntity<ApiResponse<Void, Void>> deleteQuestion(
      @PathVariable Long eventBoardId, @PathVariable Long questionId) {
    eventFormAdminService.deleteQuestion(eventBoardId, questionId);
    return ResponseEntity.ok(ApiResponse.ok(QUESTION_DELETED));
  }
}
