package inha.gdgoc.domain.board.notice.controller;

import static inha.gdgoc.domain.board.notice.controller.message.NoticeMessage.*;

import inha.gdgoc.domain.board.notice.dto.request.NoticeCreateRequest;
import inha.gdgoc.domain.board.notice.dto.request.NoticeSearchCondition;
import inha.gdgoc.domain.board.notice.dto.request.NoticeUpdateRequest;
import inha.gdgoc.domain.board.notice.dto.response.DeletedNoticeListResponse;
import inha.gdgoc.domain.board.notice.dto.response.NoticeDetailResponse;
import inha.gdgoc.domain.board.notice.dto.response.NoticeListResponse;
import inha.gdgoc.domain.board.notice.enums.CategoryEnum;
import inha.gdgoc.domain.board.notice.enums.SearchTypeEnum;
import inha.gdgoc.domain.board.notice.service.NoticeService;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.dto.response.PageMeta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/board/notices")
@RequiredArgsConstructor
public class NoticeController {

    private static final String MEMBER_OR_HIGHER_RULE =
            "@accessGuard.check(authentication,"
                    + " T(inha.gdgoc.global.security.AccessGuard$AccessCondition).atLeast("
                    + "T(inha.gdgoc.domain.user.enums.UserRole).MEMBER))";

    private static final String CORE_OR_HIGHER_RULE =
            "@accessGuard.check(authentication,"
                    + " T(inha.gdgoc.global.security.AccessGuard$AccessCondition).atLeast("
                    + "T(inha.gdgoc.domain.user.enums.UserRole).CORE))";

    private static final String LEAD_OR_HIGHER_RULE =
            "@accessGuard.check(authentication,"
                    + " T(inha.gdgoc.global.security.AccessGuard$AccessCondition).atLeast("
                    + "T(inha.gdgoc.domain.user.enums.UserRole).LEAD))";

    private final NoticeService noticeService;

    @PreAuthorize(MEMBER_OR_HIGHER_RULE)
    @GetMapping
    public ResponseEntity<ApiResponse<NoticeListResponse, Void>> listNotices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) CategoryEnum category,
            @RequestParam(required = false) SearchTypeEnum searchType,
            @RequestParam(required = false) String keyword) {
        NoticeSearchCondition condition = new NoticeSearchCondition(category, searchType, keyword);
        NoticeListResponse result = noticeService.listNotices(page, size, condition);
        return ResponseEntity.ok(ApiResponse.ok(NOTICE_LIST_RETRIEVED, result));
    }

    @PreAuthorize(MEMBER_OR_HIGHER_RULE)
    @GetMapping("/{articleId}")
    public ResponseEntity<ApiResponse<NoticeDetailResponse, Void>> getNotice(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable UUID articleId) {
        NoticeDetailResponse result = noticeService.getNotice(articleId, me.getRole(), me.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(NOTICE_RETRIEVED, result));
    }

    @PreAuthorize(CORE_OR_HIGHER_RULE)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UUID, Void>> createNotice(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestPart("request") @Valid NoticeCreateRequest request,
            @RequestPart(value = "files", required = false) MultipartFile[] files,
            @RequestPart(value = "images", required = false) MultipartFile[] images) {
        UUID articleId = noticeService.createNotice(request, files, images, me.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(NOTICE_CREATED, articleId));
    }

    @PreAuthorize(MEMBER_OR_HIGHER_RULE)
    @PatchMapping(value = "/{articleId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void, Void>> updateNotice(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable UUID articleId,
            @RequestPart("request") @Valid NoticeUpdateRequest request,
            @RequestPart(value = "files", required = false) MultipartFile[] files,
            @RequestPart(value = "images", required = false) MultipartFile[] images) {
        noticeService.updateNotice(articleId, request, files, images, me.getRole(), me.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(NOTICE_UPDATED));
    }

    @PreAuthorize(MEMBER_OR_HIGHER_RULE)
    @DeleteMapping("/{articleId}")
    public ResponseEntity<ApiResponse<Void, Void>> deleteNotice(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable UUID articleId) {
        noticeService.deleteNotice(articleId, me.getRole(), me.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(NOTICE_DELETED));
    }

    @PreAuthorize(LEAD_OR_HIGHER_RULE)
    @GetMapping("/deleted")
    public ResponseEntity<ApiResponse<Page<DeletedNoticeListResponse>, PageMeta>> listDeletedNotices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) CategoryEnum category,
            @RequestParam(required = false) SearchTypeEnum searchType,
            @RequestParam(required = false) String keyword) {
        NoticeSearchCondition condition = new NoticeSearchCondition(category, searchType, keyword);
        Page<DeletedNoticeListResponse> result = noticeService.listDeletedNotices(page, size, condition);
        return ResponseEntity.ok(ApiResponse.ok(NOTICE_DELETED_LIST_RETRIEVED, result, PageMeta.of(result)));
    }

    @PreAuthorize(LEAD_OR_HIGHER_RULE)
    @PatchMapping("/{articleId}/restore")
    public ResponseEntity<ApiResponse<Void, Void>> restoreNotice(
            @AuthenticationPrincipal CustomUserDetails me,
            @PathVariable UUID articleId) {
        noticeService.restoreNotice(articleId, me.getRole(), me.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(NOTICE_RESTORED));
    }
}
