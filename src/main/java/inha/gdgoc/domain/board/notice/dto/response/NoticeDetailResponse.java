package inha.gdgoc.domain.board.notice.dto.response;

import inha.gdgoc.domain.board.common.dto.AttachmentResponse;
import inha.gdgoc.domain.board.notice.enums.NoticeCategory;
import java.time.Instant;
import java.util.List;

public record NoticeDetailResponse(
    Long id,
    NoticeCategory category,
    String title,
    String content,
    String authorName,
    int viewCount,
    boolean isPublished,
    List<AttachmentResponse> attachments,
    Instant createdAt,
    Instant updatedAt) {}
