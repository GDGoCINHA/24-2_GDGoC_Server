package inha.gdgoc.domain.club.activity.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 회차별 필요 참석 인원 = {@code 올림(명단 수 × 기수 참석 비율)}.
 *
 * <p>{@code BigDecimal} 로 계산한다. double 로 하면 0.7 × 10 이 7.000…1 이 되어 8 로 올라간다. C 의 완주 계산도 같은 값을 써야
 * 하므로 이 메서드를 부른다.
 */
public final class RequiredAttendance {

  private RequiredAttendance() {}

  public static long of(long rosterCount, BigDecimal attendanceRatio) {
    return attendanceRatio
        .multiply(BigDecimal.valueOf(rosterCount))
        .setScale(0, RoundingMode.CEILING)
        .longValueExact();
  }
}
