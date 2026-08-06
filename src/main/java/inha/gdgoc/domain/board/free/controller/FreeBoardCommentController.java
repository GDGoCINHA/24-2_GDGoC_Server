package inha.gdgoc.domain.board.free.controller;

import static inha.gdgoc.domain.board.free.controller.message.FreeBoardMessage.*;

import inha.gdgoc.domain.board.free.dto.request.FreeBoardCommentCreateRequest;
import inha.gdgoc.domain.board.free.dto.request.FreeBoardCommentUpdateRequest;
import inha.gdgoc.domain.board.free.dto.response.FreeBoardCommentResponse;
import inha.gdgoc.domain.board.free.service.FreeBoardCommentService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 자유게시판 댓글. 권한은 글과 같다 — 조회는 회원 전용, 작성은 MEMBER 이상, 수정·삭제는 작성자 본인 또는
 * ORGANIZER 이상이다.
 *
 * <p>수정·삭제 경로에 글 id 를 두지 않는다. 댓글 id 만으로 대상이 정해지고, 글 id 를 함께 받으면 둘이 어긋났을 때의
 * 처리를 또 정해야 한다. 목록·작성만 글에 매단다.
 */
@RestController
@RequestMapping("/api/v1/board/free")
@RequiredArgsConstructor
@Validated
public class FreeBoardCommentController {

  private final FreeBoardCommentService freeBoardCommentService;

  @GetMapping("/{postId}/comments")
  public ResponseEntity<ApiResponse<List<FreeBoardCommentResponse>, Void>> listComments(
      @PathVariable Long postId) {
    return ResponseEntity.ok(
        ApiResponse.ok(FREE_COMMENT_LIST_RETRIEVED, freeBoardCommentService.listComments(postId)));
  }

  @Authorize(@Condition(atLeast = UserRole.MEMBER))
  @PostMapping("/{postId}/comments")
  public ResponseEntity<ApiResponse<Long, Void>> createComment(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long postId,
      @Valid @RequestBody FreeBoardCommentCreateRequest req) {

    Long id = freeBoardCommentService.createComment(postId, req, me.getUserId());
    return ResponseEntity.ok(ApiResponse.ok(FREE_COMMENT_CREATED, id));
  }

  @Authorize(@Condition(atLeast = UserRole.MEMBER))
  @PatchMapping("/comments/{commentId}")
  public ResponseEntity<ApiResponse<Void, Void>> updateComment(
      @AuthenticationPrincipal CustomUserDetails me,
      @PathVariable Long commentId,
      @Valid @RequestBody FreeBoardCommentUpdateRequest req) {

    freeBoardCommentService.updateComment(commentId, req, me.getUserId(), me.getRole());
    return ResponseEntity.ok(ApiResponse.ok(FREE_COMMENT_UPDATED));
  }

  @Authorize(@Condition(atLeast = UserRole.MEMBER))
  @DeleteMapping("/comments/{commentId}")
  public ResponseEntity<ApiResponse<Void, Void>> deleteComment(
      @AuthenticationPrincipal CustomUserDetails me, @PathVariable Long commentId) {

    freeBoardCommentService.deleteComment(commentId, me.getUserId(), me.getRole());
    return ResponseEntity.ok(ApiResponse.ok(FREE_COMMENT_DELETED));
  }
}
