package inha.gdgoc.domain.board.notice.dto.response;

import inha.gdgoc.domain.board.notice.enums.ArticleStatusEnum;
import inha.gdgoc.domain.board.notice.enums.CategoryEnum;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NoticeListResponse(
    long totalCount,
    int totalPages,
    int currentPage,
    List<NoticeSummaryEntry> pinnedNotices,
    List<NoticeSummaryEntry> noticeArticles
) {
    public record NoticeSummaryEntry(
        UUID articleId,
        Long articleNumber,
        CategoryEnum category,
        String title,
        String postedByName,
        Instant createdAt,
        int viewCount,
        boolean isPinned,
        ArticleStatusEnum status
    ) {}
}
