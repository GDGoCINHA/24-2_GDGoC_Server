package inha.gdgoc.domain.board.free.dto.response;

import java.time.Instant;

/** 목록 한 행. */
public record FreeBoardSummaryResponse(
    Long id, String title, String authorName, int viewCount, Instant createdAt) {}
