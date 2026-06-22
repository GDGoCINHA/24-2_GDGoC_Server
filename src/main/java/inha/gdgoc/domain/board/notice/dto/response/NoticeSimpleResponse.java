package inha.gdgoc.domain.board.notice.dto.response;

import inha.gdgoc.domain.board.notice.enums.CategoryEnum;

import java.time.Instant;
import java.util.UUID;

public record NoticeSimpleResponse(
    UUID articleId,
    Long articleNumber,
    CategoryEnum category,
    String title,
    String postedByName,
    Instant createdAt,
    int viewCount
) {}
