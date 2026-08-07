package inha.gdgoc.domain.board.free.dto.request;

import inha.gdgoc.domain.board.common.dto.AttachmentEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FreeBoardCreateRequest(
    @NotBlank @Size(max = 255) String title,
    @NotBlank String content,
    @Valid List<AttachmentEntry> attachments) {}
