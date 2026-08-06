package inha.gdgoc.domain.board.event.controller;

import static inha.gdgoc.domain.board.event.controller.message.EventBoardMessage.*;

import inha.gdgoc.domain.board.event.dto.request.EventBoardCreateRequest;
import inha.gdgoc.domain.board.event.dto.request.EventBoardUpdateRequest;
import inha.gdgoc.domain.board.event.dto.response.DeletedEventBoardSummaryResponse;
import inha.gdgoc.domain.board.event.dto.response.EventBoardDetailResponse;
import inha.gdgoc.domain.board.event.dto.response.EventBoardSummaryResponse;
import inha.gdgoc.domain.board.common.enums.SearchType;
import inha.gdgoc.domain.board.event.service.EventBoardService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.dto.response.PageMeta;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/board/events")
@RequiredArgsConstructor
@Validated
public class EventBoardController {

  private final EventBoardService eventBoardService;

  @GetMapping
  public ResponseEntity<ApiResponse<Page<EventBoardSummaryResponse>, PageMeta>> listEventBoards(
      @AuthenticationPrincipal CustomUserDetails me,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size,
      @RequestParam(defaultValue = "TITLE_AND_CONTENT") SearchType searchType,
      @RequestParam(required = false) String keyword) {
    Page<EventBoardSummaryResponse> result =
        eventBoardService.listEventBoards(
            page, size, searchType, keyword, me != null ? me.getTeam() : null, me != null ? me.getRole() : null);
    return ResponseEntity.ok(
        ApiResponse.ok(EVENT_BOARD_LIST_RETRIEVED, result, PageMeta.of(result)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<EventBoardDetailResponse, Void>> getEventBoard(
      @AuthenticationPrincipal CustomUserDetails me, @PathVariable Long id) {
    return ResponseEntity.ok(
        ApiResponse.ok(
            EVENT_BOARD_RETRIEVED,
            eventBoardService.getEventBoard(id, me != null ? me.getTeam() : null, me != null ? me.getRole() : null)));
  }

  @Authorize(@Condition(atLeast = UserRole.CORE))
  @PostMapping
  public ResponseEntity<ApiResponse<Long, Void>> createEventBoard(
      @AuthenticationPrincipal CustomUserDetails me,
      @Valid @RequestBody EventBoardCreateRequest req) {
    Long id = eventBoardService.createEventBoard(req, me.getUserId());
    return ResponseEntity.ok(ApiResponse.ok(EVENT_BOARD_CREATED, id));
  }

  @Authorize(@Condition(atLeast = UserRole.CORE))
  @PatchMapping("/{id}")
  public ResponseEntity<ApiResponse<Void, Void>> updateEventBoard(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long id, @Valid @RequestBody EventBoardUpdateRequest req) {
    eventBoardService.updateEventBoard(id, req, me.getRole(), me.getTeam());
    return ResponseEntity.ok(ApiResponse.ok(EVENT_BOARD_UPDATED));
  }

  @Authorize(@Condition(atLeast = UserRole.CORE))
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void, Void>> deleteEventBoard(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long id) {
    eventBoardService.deleteEventBoard(id, me.getRole(), me.getTeam());
    return ResponseEntity.ok(ApiResponse.ok(EVENT_BOARD_DELETED));
  }

  @Authorize(@Condition(atLeast = UserRole.CORE))
  @GetMapping("/deleted")
  public ResponseEntity<ApiResponse<Page<DeletedEventBoardSummaryResponse>, PageMeta>> listDeletedBoards(
      @AuthenticationPrincipal CustomUserDetails me,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size) {
    Page<DeletedEventBoardSummaryResponse> result =
        eventBoardService.listDeletedBoards(page, size, me.getRole(), me.getTeam());
    return ResponseEntity.ok(
        ApiResponse.ok(EVENT_BOARD_DELETED_LIST_RETRIEVED, result, PageMeta.of(result)));
  }

  @Authorize(@Condition(atLeast = UserRole.CORE))
  @PostMapping("/{id}/restore")
  public ResponseEntity<ApiResponse<Void, Void>> restoreEventBoard(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long id) {
    eventBoardService.restoreEventBoard(id, me.getRole(), me.getTeam());
    return ResponseEntity.ok(ApiResponse.ok(EVENT_BOARD_RESTORED));
  }
}
