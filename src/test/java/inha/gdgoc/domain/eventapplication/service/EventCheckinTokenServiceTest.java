package inha.gdgoc.domain.eventapplication.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * QR 토큰의 회전 규칙.
 *
 * <p>이 규칙이 무너지면 QR 사진을 받은 사람이 오지 않고도 출석 처리된다.
 */
class EventCheckinTokenServiceTest {

  private static final String SECRET = "test-secret-for-checkin";
  private static final Instant BASE = Instant.parse("2026-09-01T03:00:00Z");

  /** 창 길이. 서비스의 WINDOW_SECONDS 와 같아야 한다. */
  private static final long WINDOW = 180;

  @Test
  @DisplayName("같은 창 안에서는 같은 토큰이 나온다")
  void stableWithinWindow() {
    EventCheckinTokenService atStart = service(BASE);
    EventCheckinTokenService atEnd = service(BASE.plusSeconds(WINDOW - 1));

    assertThat(atStart.issue(1L)).isEqualTo(atEnd.issue(1L));
  }

  @Test
  @DisplayName("창이 지나면 토큰이 바뀐다")
  void rotatesEveryWindow() {
    assertThat(service(BASE).issue(1L)).isNotEqualTo(service(BASE.plusSeconds(WINDOW)).issue(1L));
  }

  @Test
  @DisplayName("행사마다 토큰이 다르다")
  void differsPerForm() {
    EventCheckinTokenService service = service(BASE);

    assertThat(service.issue(1L)).isNotEqualTo(service.issue(2L));
  }

  @Test
  @DisplayName("직전 창에 발급된 토큰도 받아준다")
  void acceptsPreviousWindow() {
    String issuedEarlier = service(BASE.plusSeconds(WINDOW - 1)).issue(1L);

    // 창이 끝나갈 때 QR 을 찍고 1초 뒤에 요청이 도착하는 것은 정상적인 상황이다.
    EventCheckinTokenService oneSecondLater = service(BASE.plusSeconds(WINDOW + 1));

    assertThat(oneSecondLater.verify(1L, issuedEarlier)).isTrue();
  }

  // 이 테스트가 지키는 것이 창을 늘린 이유 자체다. 창이 끝나갈 때 찍어도 최소 WINDOW 초가 남는다.
  @Test
  @DisplayName("창이 끝나갈 때 찍어도 로그인 왕복만큼은 버틴다")
  void survivesLoginRoundTrip() {
    String scannedAtWindowEnd = service(BASE.plusSeconds(WINDOW - 1)).issue(1L);

    EventCheckinTokenService almostThreeMinutesLater =
        service(BASE.plusSeconds(WINDOW - 1 + 175));

    assertThat(almostThreeMinutesLater.verify(1L, scannedAtWindowEnd)).isTrue();
  }

  @Test
  @DisplayName("두 번 이상 지난 토큰은 거절한다")
  void rejectsOlderThanOneWindow() {
    String old = service(BASE).issue(1L);

    // 사진으로 전달된 QR 이 걸리는 지점이다.
    assertThat(service(BASE.plusSeconds(WINDOW * 3)).verify(1L, old)).isFalse();
  }

  @Test
  @DisplayName("다른 행사의 토큰으로는 체크인할 수 없다")
  void rejectsTokenOfAnotherForm() {
    EventCheckinTokenService service = service(BASE);

    assertThat(service.verify(2L, service.issue(1L))).isFalse();
  }

  @Test
  @DisplayName("비어 있거나 엉뚱한 토큰을 거절한다")
  void rejectsGarbage() {
    EventCheckinTokenService service = service(BASE);

    assertThat(service.verify(1L, null)).isFalse();
    assertThat(service.verify(1L, "")).isFalse();
    assertThat(service.verify(1L, "AAAAAAAAAA")).isFalse();
  }

  @Test
  @DisplayName("비밀키가 다르면 서로의 토큰을 받지 않는다")
  void differentSecretsDoNotInterop() {
    EventCheckinTokenService other =
        new EventCheckinTokenService("another-secret", Clock.fixed(BASE, ZoneOffset.UTC));

    assertThat(other.verify(1L, service(BASE).issue(1L))).isFalse();
  }

  @Test
  @DisplayName("남은 초는 다음 창까지의 거리다")
  void remainingSecondsCountsDown() {
    assertThat(service(BASE).remainingSeconds()).isEqualTo(WINDOW);
    assertThat(service(BASE.plusSeconds(45)).remainingSeconds()).isEqualTo(WINDOW - 45);
  }

  private static EventCheckinTokenService service(Instant now) {
    return new EventCheckinTokenService(SECRET, Clock.fixed(now, ZoneOffset.UTC));
  }
}
