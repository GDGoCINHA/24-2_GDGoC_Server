package inha.gdgoc.domain.board.notice.dto.response;

import inha.gdgoc.domain.board.notice.enums.NoticeCategory;
import java.time.Instant;

/** 목록 한 행. 고정 모달의 리스트 행도 같은 필드를 쓴다. */
public record NoticeSummaryResponse(
    Long id,
    NoticeCategory category,
    String title,
    String authorName,
    int viewCount,
    boolean isPublished,
    Instant createdAt) {}
