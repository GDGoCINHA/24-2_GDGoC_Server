package inha.gdgoc.domain.club.activity.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 활동 기록 작성 화면의 명단. 활동일 당시 팀 명단이며, 제출하면 이 사람들이 그대로 출석 행이 된다.
 *
 * @param requiredCount 이 회차가 인정 활동이 되려면 필요한 출석 수
 */
public record ClubActivityRosterResponse(
    LocalDate date, BigDecimal attendanceRatio, long requiredCount, List<Member> members) {

  /**
   * @param checkedIn QR 로 체크인했는가. 화면이 출석 체크의 기본값으로 쓴다 — 최종 출석은 리더가 제출한 값이다
   */
  public record Member(Long userId, String name, boolean leader, boolean checkedIn) {}
}
