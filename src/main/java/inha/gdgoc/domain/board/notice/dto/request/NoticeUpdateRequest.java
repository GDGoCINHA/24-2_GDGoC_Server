package inha.gdgoc.domain.board.notice.dto.request;

import inha.gdgoc.domain.board.common.dto.AttachmentEntry;
import inha.gdgoc.domain.board.notice.enums.NoticeCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 모든 필드가 선택이다. null 인 필드는 바꾸지 않는다. */
public record NoticeUpdateRequest(
    @Size(max = 255) String title,
    String content,
    NoticeCategory category,
    Boolean isPublished,
    @Valid List<AttachmentEntry> attachments) {}
