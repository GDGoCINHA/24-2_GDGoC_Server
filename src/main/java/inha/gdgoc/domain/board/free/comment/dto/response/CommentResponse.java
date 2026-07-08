package inha.gdgoc.domain.board.free.comment.dto.response;

import inha.gdgoc.domain.board.free.comment.entity.Comment;
import java.time.Instant;

// 댓글 작성/수정/목록 응답
public record CommentResponse(
        Long id,
        Long postId,
        String content,
        Long authorId,
        String authorName,
        Instant createdAt,
        Instant updatedAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getContent(),
                comment.getAuthor().getId(),
                comment.getAuthor().getName(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
