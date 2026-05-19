package inha.gdgoc.domain.board.event.dto.request;

import inha.gdgoc.domain.user.enums.TeamType;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

public record EventBoardUpdateRequest(
    String title,
    LocalDate eventStartDate,
    LocalDate eventEndDate,
    TeamType organizingTeam,
    String thumbnailKey,
    String content,
    Boolean isPublished,
    @Valid List<EventBoardCreateRequest.AttachmentEntry> attachments) {}
