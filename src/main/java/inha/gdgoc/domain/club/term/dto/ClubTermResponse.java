package inha.gdgoc.domain.club.term.dto;

import inha.gdgoc.domain.club.club.entity.ClubTerm;
import java.math.BigDecimal;

public record ClubTermResponse(Long id, String name, BigDecimal attendanceRatio) {

  public static ClubTermResponse from(ClubTerm term) {
    return new ClubTermResponse(term.getId(), term.getName(), term.getAttendanceRatio());
  }
}
