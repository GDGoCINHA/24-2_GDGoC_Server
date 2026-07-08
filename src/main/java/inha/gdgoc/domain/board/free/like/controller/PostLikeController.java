package inha.gdgoc.domain.board.free.like.controller;

import inha.gdgoc.domain.board.free.like.controller.message.LikeMessage;
import inha.gdgoc.domain.board.free.like.dto.response.LikeResponse;
import inha.gdgoc.domain.board.free.like.service.PostLikeService;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/board/free/{postId}/likes")
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeService service;

    /* ---------- 좋아요 ---------- */
    @PostMapping
    public ResponseEntity<ApiResponse<LikeResponse, Void>> like(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestHeader(value = "X-Debug-User-Id", required = false) Long debugUserId,
            @PathVariable Long postId) {
        LikeResponse res = service.like(resolveUserId(me, debugUserId), postId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(res, LikeMessage.LIKE_ADDED_SUCCESS));
    }

    /* ---------- 좋아요 취소 ---------- */
    @DeleteMapping
    public ResponseEntity<ApiResponse<LikeResponse, Void>> unlike(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestHeader(value = "X-Debug-User-Id", required = false) Long debugUserId,
            @PathVariable Long postId) {
        LikeResponse res = service.unlike(resolveUserId(me, debugUserId), postId);
        return ResponseEntity.ok(ApiResponse.ok(LikeMessage.LIKE_REMOVED_SUCCESS, res));
    }

    /* ---------- 좋아요 수/상태 조회 ---------- */
    @GetMapping
    public ResponseEntity<ApiResponse<LikeResponse, Void>> status(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestHeader(value = "X-Debug-User-Id", required = false) Long debugUserId,
            @PathVariable Long postId) {
        LikeResponse res = service.status(resolveOptionalUserId(me, debugUserId), postId);
        return ResponseEntity.ok(ApiResponse.ok(LikeMessage.LIKE_STATUS_RETRIEVED_SUCCESS, res));
    }

    private Long resolveUserId(CustomUserDetails me, Long debugUserId) {
        if (me != null) {
            return me.getUserId();
        }
        if (debugUserId != null) {
            return debugUserId;
        }
        throw new BusinessException(GlobalErrorCode.UNAUTHORIZED_USER);
    }

    // 상태 조회
    private Long resolveOptionalUserId(CustomUserDetails me, Long debugUserId) {
        if (me != null) {
            return me.getUserId();
        }
        return debugUserId;
    }
}
