package inha.gdgoc.domain.board.free.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 댓글 작성.
 *
 * <p>parentId 가 있으면 대댓글이다. 대댓글의 id 를 넘겨도 되고, 그때는 그 대댓글의 부모에 붙는다 — 깊이는 항상
 * 1단계다({@code FreeBoardComment.resolveParent}).
 */
public record FreeBoardCommentCreateRequest(
    @NotBlank @Size(max = 1000) String content, Long parentId) {}
