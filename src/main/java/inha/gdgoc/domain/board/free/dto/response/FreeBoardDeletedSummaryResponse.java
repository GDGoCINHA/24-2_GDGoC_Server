package inha.gdgoc.domain.board.free.dto.response;

import java.time.Instant;

/** 삭제 목록 한 행. deletedAt 이 정렬 기준이라 응답에도 노출한다. */
public record FreeBoardDeletedSummaryResponse(
    Long id,
    String title,
    String authorName,
    int viewCount,
    Instant createdAt,
    Instant deletedAt) {}
