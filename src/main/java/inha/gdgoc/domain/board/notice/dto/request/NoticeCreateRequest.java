package inha.gdgoc.domain.board.notice.dto.request;

import inha.gdgoc.domain.board.common.dto.AttachmentEntry;
import inha.gdgoc.domain.board.notice.enums.NoticeCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record NoticeCreateRequest(
    @NotBlank @Size(max = 255) String title,
    @NotBlank String content,
    @NotNull NoticeCategory category,
    boolean isPublished,
    @Valid List<AttachmentEntry> attachments) {}
