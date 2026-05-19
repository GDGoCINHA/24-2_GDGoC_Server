package inha.gdgoc.domain.board.event.dto.request;

import inha.gdgoc.domain.user.enums.TeamType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record EventBoardCreateRequest(
    @NotBlank String title,
    @NotNull LocalDate eventStartDate,
    @NotNull LocalDate eventEndDate,
    @NotNull TeamType organizingTeam,
    String thumbnailKey,
    @NotBlank String content,
    boolean isPublished,
    @Valid List<AttachmentEntry> attachments) {

  public record AttachmentEntry(@NotBlank String fileKey, @NotBlank String fileName) {}
}
