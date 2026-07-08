package inha.gdgoc.domain.board.free.like.dto.response;

public record LikeResponse(
                Long postId,
                long likeCount,
                boolean liked) {
}
