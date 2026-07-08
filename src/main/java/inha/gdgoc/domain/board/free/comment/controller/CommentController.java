package inha.gdgoc.domain.board.free.comment.controller;

import inha.gdgoc.domain.board.free.comment.controller.message.CommentMessage;
import inha.gdgoc.domain.board.free.comment.dto.request.CommentCreateRequest;
import inha.gdgoc.domain.board.free.comment.dto.request.CommentUpdateRequest;
import inha.gdgoc.domain.board.free.comment.dto.response.CommentResponse;
import inha.gdgoc.domain.board.free.comment.service.CommentService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/board/free/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService service;

    /* ---------- Create ---------- */
    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse, Void>> create(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestHeader(value = "X-Debug-User-Id", required = false) Long debugUserId,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest req) {
        CommentResponse created = service.create(resolveUserId(me, debugUserId), postId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, CommentMessage.COMMENT_CREATED_SUCCESS));
    }

    /* ---------- Read ---------- */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CommentResponse>, Void>> list(
            @PathVariable Long postId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<CommentResponse> page = service.list(postId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(CommentMessage.COMMENT_LIST_RETRIEVED_SUCCESS, page));
    }

    /* ---------- Update ---------- */
    @PatchMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse, Void>> update(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestHeader(value = "X-Debug-User-Id", required = false) Long debugUserId,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest req) {
        CommentResponse updated = service.update(resolveUserId(me, debugUserId), resolveRole(me), postId, commentId,
                req);
        return ResponseEntity.ok(ApiResponse.ok(CommentMessage.COMMENT_UPDATED_SUCCESS, updated));
    }

    /* ---------- Delete ---------- */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Map<String, Object>, Void>> delete(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestHeader(value = "X-Debug-User-Id", required = false) Long debugUserId,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        service.delete(resolveUserId(me, debugUserId), resolveRole(me), postId, commentId);
        return ResponseEntity.ok(ApiResponse.ok(CommentMessage.COMMENT_DELETED_SUCCESS, Map.of("deleted", commentId)));
    }

    /* ---------- 인증에 사용되는 메소드 (권한, 아이디) ---------- */
    private Long resolveUserId(CustomUserDetails me, Long debugUserId) {
        if (me != null) {
            return me.getUserId();
        }
        if (debugUserId != null) {
            return debugUserId;
        }
        throw new BusinessException(GlobalErrorCode.UNAUTHORIZED_USER);
    }

    private UserRole resolveRole(CustomUserDetails me) {
        return me != null ? me.getRole() : UserRole.MEMBER; // 로컬 테스트 기본 권한
    }
}
