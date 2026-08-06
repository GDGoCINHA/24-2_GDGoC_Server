package inha.gdgoc.domain.board.free.dto.request;

import inha.gdgoc.domain.board.common.dto.AttachmentEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 모든 필드가 선택이다. null 인 필드는 바꾸지 않는다. */
public record FreeBoardUpdateRequest(
    @Size(max = 255) String title, String content, @Valid List<AttachmentEntry> attachments) {}
