package inha.gdgoc.domain.board.notice.dto.response;

import inha.gdgoc.domain.board.notice.enums.NoticeCategory;
import java.time.Instant;

/** 삭제 목록 한 행. deletedAt 이 정렬 기준이라 응답에도 노출한다. */
public record NoticeDeletedSummaryResponse(
    Long id,
    NoticeCategory category,
    String title,
    String authorName,
    int viewCount,
    boolean isPublished,
    Instant createdAt,
    Instant deletedAt) {}
