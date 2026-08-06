package inha.gdgoc.domain.board.notice.controller;

import static inha.gdgoc.domain.board.notice.controller.message.NoticeBoardMessage.*;

import inha.gdgoc.domain.board.common.enums.SearchType;
import inha.gdgoc.domain.board.notice.dto.request.NoticeCreateRequest;
import inha.gdgoc.domain.board.notice.dto.request.NoticeUpdateRequest;
import inha.gdgoc.domain.board.notice.dto.request.PinnedUpdateRequest;
import inha.gdgoc.domain.board.notice.dto.response.NoticeDeletedSummaryResponse;
import inha.gdgoc.domain.board.notice.dto.response.NoticeDetailResponse;
import inha.gdgoc.domain.board.notice.dto.response.NoticeListResponse;
import inha.gdgoc.domain.board.notice.dto.response.NoticeSummaryResponse;
import inha.gdgoc.domain.board.notice.service.NoticeBoardService;
import inha.gdgoc.domain.board.notice.service.PinnedNoticeService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.dto.response.PageMeta;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/board/notices")
@RequiredArgsConstructor
@Validated
public class NoticeBoardController {

  private final NoticeBoardService noticeBoardService;
  private final PinnedNoticeService pinnedNoticeService;

  @GetMapping
  public ResponseEntity<ApiResponse<NoticeListResponse, PageMeta>> listNotices(
      @AuthenticationPrincipal CustomUserDetails me,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size,
      @RequestParam(defaultValue = "TITLE_AND_CONTENT") SearchType searchType,
      @RequestParam(required = false) String keyword) {

    UserRole role = me != null ? me.getRole() : null;
    Page<NoticeSummaryResponse> posts =
        noticeBoardService.listNotices(page, size, searchType, keyword, role);
    List<NoticeSummaryResponse> pinned = pinnedNoticeService.getPinned();

    return ResponseEntity.ok(
        ApiResponse.ok(
            NOTICE_LIST_RETRIEVED, new NoticeListResponse(pinned, posts), PageMeta.of(posts)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<NoticeDetailResponse, Void>> getNotice(
      @AuthenticationPrincipal CustomUserDetails me, @PathVariable Long id) {
    return ResponseEntity.ok(
        ApiResponse.ok(NOTICE_RETRIEVED, noticeBoardService.getNotice(id, me != null ? me.getRole() : null)));
  }

  @Authorize(@Condition(atLeast = UserRole.CORE))
  @PostMapping
  public ResponseEntity<ApiResponse<Long, Void>> createNotice(
      @AuthenticationPrincipal CustomUserDetails me, @Valid @RequestBody NoticeCreateRequest req) {
    return ResponseEntity.ok(
        ApiResponse.ok(NOTICE_CREATED, noticeBoardService.createNotice(req, me.getUserId())));
  }

  @Authorize(@Condition(atLeast = UserRole.CORE))
  @PatchMapping("/{id}")
  public ResponseEntity<ApiResponse<Void, Void>> updateNotice(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long id,
      @Valid @RequestBody NoticeUpdateRequest req) {
    noticeBoardService.updateNotice(id, req, me.getUserId(), me.getRole());
    return ResponseEntity.ok(ApiResponse.ok(NOTICE_UPDATED));
  }

  @Authorize(@Condition(atLeast = UserRole.CORE))
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void, Void>> deleteNotice(
      @AuthenticationPrincipal CustomUserDetails me, @PathVariable Long id) {
    noticeBoardService.deleteNotice(id, me.getUserId(), me.getRole());
    return ResponseEntity.ok(ApiResponse.ok(NOTICE_DELETED));
  }

  @Authorize(@Condition(atLeast = UserRole.CORE))
  @GetMapping("/deleted")
  public ResponseEntity<ApiResponse<Page<NoticeDeletedSummaryResponse>, PageMeta>> listDeletedNotices(
      @AuthenticationPrincipal CustomUserDetails me,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size) {
    Page<NoticeDeletedSummaryResponse> result =
        noticeBoardService.listDeletedNotices(page, size, me.getUserId(), me.getRole());
    return ResponseEntity.ok(
        ApiResponse.ok(NOTICE_DELETED_LIST_RETRIEVED, result, PageMeta.of(result)));
  }

  @Authorize(@Condition(atLeast = UserRole.CORE))
  @PostMapping("/{id}/restore")
  public ResponseEntity<ApiResponse<Void, Void>> restoreNotice(
      @AuthenticationPrincipal CustomUserDetails me, @PathVariable Long id) {
    noticeBoardService.restoreNotice(id, me.getUserId(), me.getRole());
    return ResponseEntity.ok(ApiResponse.ok(NOTICE_RESTORED));
  }

  @Authorize(@Condition(atLeast = UserRole.ORGANIZER))
  @GetMapping("/pinned")
  public ResponseEntity<ApiResponse<List<NoticeSummaryResponse>, Void>> getPinned() {
    return ResponseEntity.ok(ApiResponse.ok(PINNED_RETRIEVED, pinnedNoticeService.getPinned()));
  }

  @Authorize(@Condition(atLeast = UserRole.ORGANIZER))
  @PutMapping("/pinned")
  public ResponseEntity<ApiResponse<Void, Void>> replacePinned(
      @AuthenticationPrincipal CustomUserDetails me, @Valid @RequestBody PinnedUpdateRequest req) {
    pinnedNoticeService.replacePinned(req, me.getUserId());
    return ResponseEntity.ok(ApiResponse.ok(PINNED_UPDATED));
  }
}
