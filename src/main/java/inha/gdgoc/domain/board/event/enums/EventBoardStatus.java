package inha.gdgoc.domain.board.event.enums;

import java.time.LocalDate;

public enum EventBoardStatus {
  UPCOMING,
  IN_PROGRESS,
  ENDED;

  public static EventBoardStatus of(LocalDate startDate, LocalDate endDate) {
    LocalDate today = LocalDate.now();
    if (endDate.isBefore(today)) return ENDED;
    if (startDate.isAfter(today)) return UPCOMING;
    return IN_PROGRESS;
  }
}
