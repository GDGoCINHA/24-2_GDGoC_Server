package inha.gdgoc.domain.board.event.dto.response;

import inha.gdgoc.domain.user.enums.TeamType;
import java.time.Instant;
import java.time.LocalDate;

public record DeletedEventBoardSummaryResponse(
    Long id,
    String title,
    LocalDate eventStartDate,
    LocalDate eventEndDate,
    TeamType organizingTeam,
    Instant deletedAt) {}
