package inha.gdgoc.domain.board.notice.dto.response;

import inha.gdgoc.domain.board.notice.enums.ArticleStatusEnum;
import inha.gdgoc.domain.board.notice.enums.AttachmentTypeEnum;
import inha.gdgoc.domain.board.notice.enums.CategoryEnum;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NoticeDetailResponse(
    UUID articleId,
    Long articleNumber,
    CategoryEnum category,
    String title,
    String postedByName,
    ArticleStatusEnum status,
    Instant createdAt,
    Instant updatedAt,
    Instant deletedAt,
    int viewCount,
    String content,
    List<AttachmentEntry> attachments,
    NoticeSimpleResponse prevNotice,
    NoticeSimpleResponse nextNotice
) {
    public record AttachmentEntry(
        UUID attachmentId,
        AttachmentTypeEnum attachmentType,
        String originalName,
        Long fileSize,
        String mimeType,
        String linkUrl
    ) {}
}
