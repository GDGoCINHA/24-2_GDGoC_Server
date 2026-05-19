package inha.gdgoc.domain.board.event.dto.response;

import inha.gdgoc.domain.board.event.enums.EventBoardStatus;
import inha.gdgoc.domain.user.enums.TeamType;
import java.time.LocalDate;

public record EventBoardSummaryResponse(
    Long id,
    String title,
    String thumbnailUrl,
    LocalDate eventStartDate,
    LocalDate eventEndDate,
    TeamType organizingTeam,
    String authorName,
    EventBoardStatus status) {}
