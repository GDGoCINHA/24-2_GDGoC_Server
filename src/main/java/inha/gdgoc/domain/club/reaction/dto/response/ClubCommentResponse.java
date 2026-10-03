package inha.gdgoc.domain.club.reaction.dto.response;

import java.time.Instant;

/** @param deletable 지금 보는 사람이 지울 수 있는지 (작성자·팀 리더·운영진) */
public record ClubCommentResponse(
    Long id, Long authorId, String authorName, String content, Instant createdAt, boolean deletable) {}
