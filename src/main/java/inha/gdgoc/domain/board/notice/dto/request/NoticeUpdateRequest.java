package inha.gdgoc.domain.board.notice.dto.request;

import inha.gdgoc.domain.board.notice.enums.ArticleStatusEnum;
import inha.gdgoc.domain.board.notice.enums.CategoryEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record NoticeUpdateRequest(
    CategoryEnum category,
    @Size(max = 255) String title,
    String content,
    Boolean isPinned,
    ArticleStatusEnum status,
    @Valid List<NoticeCreateRequest.UrlAttachmentEntry> urlAttachments,
    @Valid List<KeepAttachmentEntry> keepAttachmentIds
) {
    public record KeepAttachmentEntry(
        @NotNull UUID attachmentId,
        @NotNull Integer displayOrder
    ) {}
}
