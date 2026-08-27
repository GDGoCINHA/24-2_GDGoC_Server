package inha.gdgoc.domain.eventapplication.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * QR 토큰의 수명 규칙.
 *
 * <p>이 규칙이 무너지면 QR 사진을 받은 사람이 오지 않고도 출석 처리된다.
 *
 * <p>토큰이 자기 만료 시각을 들고 다니므로 <b>발급할 때마다 다른 값</b>이 나오고 수명은 항상 정확히 {@link #LIFETIME} 초다. 예전 벽시계 창
 * 방식에서는 같은 창 안에서 몇 번을 발급해도 같은 토큰이 나왔다.
 */
class EventCheckinTokenServiceTest {

  private static final String SECRET = "test-secret-for-checkin";
  private static final Instant BASE = Instant.parse("2026-09-01T03:00:00Z");

  /** 서비스의 LIFETIME_SECONDS 와 같아야 한다. */
  private static final long LIFETIME = 180;

  // 「QR 새로 받기」 가 눌러도 아무것도 안 바뀌는 버튼이었던 이유가 여기였다.
  @Test
  @DisplayName("다시 발급하면 다른 토큰이 나온다")
  void reissueGivesNewToken() {
    String first = service(BASE).issue(1L);
    String second = service(BASE.plusSeconds(1)).issue(1L);

    assertThat(second).isNotEqualTo(first);
  }

  @Test
  @DisplayName("발급 직후에는 수명만큼 남는다")
  void freshTokenHasFullLifetime() {
    assertThat(service(BASE).lifetimeSeconds()).isEqualTo(LIFETIME);
  }

  @Test
  @DisplayName("수명 안이면 받아준다")
  void acceptsWithinLifetime() {
    String token = service(BASE).issue(1L);

    assertThat(service(BASE.plusSeconds(LIFETIME - 1)).verify(1L, token)).isTrue();
  }

  // 창 방식에서는 찍은 시점에 따라 수명이 1~2배로 들쭉날쭉했다. 지금은 언제 찍어도 같다.
  @Test
  @DisplayName("만료 시각 정각까지는 받아준다")
  void acceptsAtExactExpiry() {
    String token = service(BASE).issue(1L);

    assertThat(service(BASE.plusSeconds(LIFETIME)).verify(1L, token)).isTrue();
  }

  @Test
  @DisplayName("수명이 지나면 거절한다")
  void rejectsExpired() {
    String token = service(BASE).issue(1L);

    // 사진으로 전달된 QR 이 걸리는 지점이다.
    assertThat(service(BASE.plusSeconds(LIFETIME + 1)).verify(1L, token)).isFalse();
  }

  // 로그인 왕복을 버티게 하려고 수명을 60초에서 늘렸다. 그 보장이 이 테스트다.
  @Test
  @DisplayName("구글 로그인 왕복만큼은 버틴다")
  void survivesLoginRoundTrip() {
    String token = service(BASE).issue(1L);

    assertThat(service(BASE.plusSeconds(150)).verify(1L, token)).isTrue();
  }

  @Test
  @DisplayName("행사마다 토큰이 다르다")
  void differsPerForm() {
    EventCheckinTokenService service = service(BASE);

    assertThat(service.issue(1L)).isNotEqualTo(service.issue(2L));
  }

  @Test
  @DisplayName("다른 행사의 토큰으로는 체크인할 수 없다")
  void rejectsTokenOfAnotherForm() {
    EventCheckinTokenService service = service(BASE);

    assertThat(service.verify(2L, service.issue(1L))).isFalse();
  }

  // 서명이 만료 시각까지 덮는다. 안 그러면 앞자리만 바꿔 영구 토큰을 만들 수 있다.
  @Test
  @DisplayName("만료 시각을 늘려 적으면 거절한다")
  void rejectsTamperedExpiry() {
    EventCheckinTokenService service = service(BASE);
    String token = service.issue(1L);
    String signature = token.substring(token.indexOf('.') + 1);
    long farFuture = BASE.getEpochSecond() + 86400;

    String forged = Long.toString(farFuture, 36) + "." + signature;

    assertThat(service.verify(1L, forged)).isFalse();
  }

  @Test
  @DisplayName("비어 있거나 엉뚱한 토큰을 거절한다")
  void rejectsGarbage() {
    EventCheckinTokenService service = service(BASE);

    assertThat(service.verify(1L, null)).isFalse();
    assertThat(service.verify(1L, "")).isFalse();
    assertThat(service.verify(1L, "AAAAAAAAAA")).isFalse();
    assertThat(service.verify(1L, ".")).isFalse();
    assertThat(service.verify(1L, ".AAAAAAAAAA")).isFalse();
    assertThat(service.verify(1L, "zzz.")).isFalse();
    // 36진수로 못 읽는 만료 시각
    assertThat(service.verify(1L, "!!!.AAAAAAAAAA")).isFalse();
  }

  @Test
  @DisplayName("비밀키가 다르면 서로의 토큰을 받지 않는다")
  void differentSecretsDoNotInterop() {
    EventCheckinTokenService other =
        new EventCheckinTokenService("another-secret", Clock.fixed(BASE, ZoneOffset.UTC));

    assertThat(other.verify(1L, service(BASE).issue(1L))).isFalse();
  }

  private static EventCheckinTokenService service(Instant now) {
    return new EventCheckinTokenService(SECRET, Clock.fixed(now, ZoneOffset.UTC));
  }
}
