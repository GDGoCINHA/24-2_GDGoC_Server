package inha.gdgoc.domain.board.free.dto.response;

import inha.gdgoc.domain.board.free.entity.Post;
import java.time.Instant;

// 단건 조회/작성/수정 응답 (본문 포함)
public record PostResponse(
        Long id,
        String title,
        String content,
        Long authorId,
        String authorName,
        Instant createdAt,
        Instant updatedAt
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor().getId(),
                post.getAuthor().getName(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
