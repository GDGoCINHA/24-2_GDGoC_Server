package inha.gdgoc.domain.board.free.controller;

import inha.gdgoc.domain.board.free.controller.message.PostMessage;
import inha.gdgoc.domain.board.free.dto.request.PostCreateRequest;
import inha.gdgoc.domain.board.free.dto.request.PostUpdateRequest;
import inha.gdgoc.domain.board.free.dto.response.PostResponse;
import inha.gdgoc.domain.board.free.dto.response.PostSummaryResponse;
import inha.gdgoc.domain.board.free.service.PostService;
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
@RequestMapping("/api/v1/board/free")
@RequiredArgsConstructor
public class PostController {

    private final PostService service;

    /* ---------- Create ---------- */
    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse, Void>> create(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestHeader(value = "X-Debug-User-Id", required = false) Long debugUserId,
            @Valid @RequestBody PostCreateRequest req) {
        PostResponse created = service.create(resolveUserId(me, debugUserId), req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created, PostMessage.POST_CREATED_SUCCESS));
    }

    /* ---------- Read ---------- */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostSummaryResponse>, Void>> list(
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PostSummaryResponse> page = service.list(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PostMessage.POST_LIST_RETRIEVED_SUCCESS, page));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse, Void>> get(@PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.ok(PostMessage.POST_RETRIEVED_SUCCESS, service.get(postId)));
    }

    /* ---------- Update ---------- */
    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse, Void>> update(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestHeader(value = "X-Debug-User-Id", required = false) Long debugUserId,
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest req) {
        PostResponse updated = service.update(resolveUserId(me, debugUserId), resolveRole(me), postId, req);
        return ResponseEntity.ok(ApiResponse.ok(PostMessage.POST_UPDATED_SUCCESS, updated));
    }

    /* ---------- Delete ---------- */
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Map<String, Object>, Void>> delete(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestHeader(value = "X-Debug-User-Id", required = false) Long debugUserId,
            @PathVariable Long postId) {
        service.delete(resolveUserId(me, debugUserId), resolveRole(me), postId);
        return ResponseEntity.ok(ApiResponse.ok(PostMessage.POST_DELETED_SUCCESS, Map.of("deleted", postId)));
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
