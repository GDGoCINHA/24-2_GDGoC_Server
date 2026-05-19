package inha.gdgoc.domain.board.event.dto.response;

import inha.gdgoc.domain.board.event.enums.EventBoardStatus;
import inha.gdgoc.domain.user.enums.TeamType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record EventBoardDetailResponse(
    Long id,
    String title,
    LocalDate eventStartDate,
    LocalDate eventEndDate,
    TeamType organizingTeam,
    String authorName,
    String thumbnailUrl,
    String content,
    boolean isPublished,
    EventBoardStatus status,
    List<AttachmentResponse> attachments,
    Instant createdAt,
    Instant updatedAt) {

  public record AttachmentResponse(Long id, String fileUrl, String fileName) {}
}
