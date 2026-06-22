package inha.gdgoc.domain.board.notice.dto.response;

import inha.gdgoc.domain.board.notice.enums.CategoryEnum;

import java.time.Instant;
import java.util.UUID;

public record DeletedNoticeListResponse(
    UUID articleId,
    Long articleNumber,
    CategoryEnum category,
    String title,
    String postedByName,
    Instant deletedAt,
    int viewCount,
    boolean isPinned
) {}
