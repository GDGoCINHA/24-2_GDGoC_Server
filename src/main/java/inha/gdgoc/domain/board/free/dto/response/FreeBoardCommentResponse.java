package inha.gdgoc.domain.board.free.dto.response;

import inha.gdgoc.domain.board.free.entity.FreeBoardComment;
import java.time.Instant;
import java.util.List;

/**
 * 댓글 한 건. 최상위 댓글만 replies 를 갖고, 대댓글의 replies 는 항상 빈 배열이다 — 깊이가 1단계다.
 *
 * <p>삭제된 댓글은 자식이 남아 있을 때만 응답에 실린다. 그때 content·authorId·authorName 은 전부 null 이다.
 * 지운 사람이 누구였는지까지 남길 이유가 없다. 화면 문구('삭제된 댓글입니다')는 클라이언트가 정한다.
 */
public record FreeBoardCommentResponse(
    Long id,
    Long parentId,
    String content,
    Long authorId,
    String authorName,
    boolean deleted,
    Instant createdAt,
    Instant updatedAt,
    List<FreeBoardCommentResponse> replies) {

  public static FreeBoardCommentResponse of(
      FreeBoardComment comment, List<FreeBoardCommentResponse> replies) {

    if (comment.isDeleted()) {
      return new FreeBoardCommentResponse(
          comment.getId(),
          comment.getParent() == null ? null : comment.getParent().getId(),
          null,
          null,
          null,
          true,
          comment.getCreatedAt(),
          comment.getUpdatedAt(),
          replies);
    }

    return new FreeBoardCommentResponse(
        comment.getId(),
        comment.getParent() == null ? null : comment.getParent().getId(),
        comment.getContent(),
        comment.getAuthorId(),
        comment.getAuthorName(),
        false,
        comment.getCreatedAt(),
        comment.getUpdatedAt(),
        replies);
  }
}
