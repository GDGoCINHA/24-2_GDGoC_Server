package inha.gdgoc.domain.club.activity.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 회차별 필요 참석 인원. 하나 차이로 인정 활동 여부가 갈린다. */
class RequiredAttendanceTest {

  @Test
  @DisplayName("나누어떨어지지 않으면 올린다")
  void roundsUp() {
    assertThat(RequiredAttendance.of(5, new BigDecimal("0.50"))).isEqualTo(3);
    assertThat(RequiredAttendance.of(3, new BigDecimal("0.70"))).isEqualTo(3);
  }

  @Test
  @DisplayName("딱 떨어지면 그대로다 — 부동소수 오차로 하나 더 요구하지 않는다")
  void exactProductIsNotRoundedUp() {
    // double 이면 0.7 * 10 = 7.000000000000001 이라 올림하면 8 이 된다.
    assertThat(RequiredAttendance.of(10, new BigDecimal("0.70"))).isEqualTo(7);
    assertThat(RequiredAttendance.of(4, new BigDecimal("0.50"))).isEqualTo(2);
  }

  @Test
  @DisplayName("명단이 비면 0 이다")
  void emptyRoster() {
    assertThat(RequiredAttendance.of(0, new BigDecimal("0.50"))).isZero();
  }
}
