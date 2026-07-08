package inha.gdgoc.domain.board.free.dto.response;

import inha.gdgoc.domain.board.free.entity.Post;
import java.time.Instant;

// 목록 응답 (본문 제외 - 가벼운 요약)
public record PostSummaryResponse(
        Long id,
        String title,
        Long authorId,
        String authorName,
        Instant createdAt
) {
    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getAuthor().getId(),
                post.getAuthor().getName(),
                post.getCreatedAt()
        );
    }
}
