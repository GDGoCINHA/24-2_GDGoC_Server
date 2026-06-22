package inha.gdgoc.domain.board.notice.dto.request;

import inha.gdgoc.domain.board.notice.enums.ArticleStatusEnum;
import inha.gdgoc.domain.board.notice.enums.CategoryEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record NoticeCreateRequest(
    @NotNull CategoryEnum category,
    @NotBlank @Size(max = 255) String title,
    @NotBlank String content,
    Boolean isPinned,
    @NotNull ArticleStatusEnum status,
    @Valid List<UrlAttachmentEntry> urlAttachments
) {
    public record UrlAttachmentEntry(
        @NotBlank String linkUrl,
        @NotNull Integer displayOrder
    ) {}
}
