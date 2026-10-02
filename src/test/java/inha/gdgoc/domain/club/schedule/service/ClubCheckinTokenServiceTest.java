package inha.gdgoc.domain.club.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** QR 토큰. 위조되면 모임에 안 온 사람이 출석 기본값을 얻는다. */
class ClubCheckinTokenServiceTest {

  private static final String SECRET = "test-secret";
  private static final Instant ISSUED = Instant.parse("2026-10-07T10:00:00Z");
  private static final long SCHEDULE_ID = 42L;

  private static ClubCheckinTokenService at(Instant now) {
    return new ClubCheckinTokenService(SECRET, Clock.fixed(now, ZoneOffset.UTC));
  }

  @Test
  @DisplayName("발급한 토큰은 그 일정 id 로 풀린다")
  void roundTrip() {
    String token = at(ISSUED).issue(SCHEDULE_ID);

    assertThat(at(ISSUED.plusSeconds(179)).verify(token)).contains(SCHEDULE_ID);
  }

  @Test
  @DisplayName("3분이 지나면 무효다")
  void expired() {
    String token = at(ISSUED).issue(SCHEDULE_ID);

    assertThat(at(ISSUED.plusSeconds(181)).verify(token)).isEmpty();
  }

  @Test
  @DisplayName("일정 id 를 바꿔 적으면 서명이 맞지 않는다")
  void swappedScheduleIdIsRejected() {
    String token = at(ISSUED).issue(SCHEDULE_ID);
    String forged = Long.toString(SCHEDULE_ID + 1, 36) + token.substring(token.indexOf('.'));

    assertThat(at(ISSUED).verify(forged)).isEmpty();
  }

  @Test
  @DisplayName("만료 시각을 늘려 적으면 서명이 맞지 않는다")
  void extendedExpiryIsRejected() {
    String token = at(ISSUED).issue(SCHEDULE_ID);
    String[] parts = token.split("\\.");
    long longer = Long.parseLong(parts[1], 36) + 3600;
    String forged = parts[0] + "." + Long.toString(longer, 36) + "." + parts[2];

    assertThat(at(ISSUED.plusSeconds(600)).verify(forged)).isEmpty();
  }

  @Test
  @DisplayName("다른 키로 서명한 토큰은 무효다")
  void otherSecretIsRejected() {
    String token =
        new ClubCheckinTokenService("other-secret", Clock.fixed(ISSUED, ZoneOffset.UTC))
            .issue(SCHEDULE_ID);

    assertThat(at(ISSUED).verify(token)).isEmpty();
  }

  @Test
  @DisplayName("형식이 틀린 토큰은 예외 없이 무효다")
  void malformedTokens() {
    ClubCheckinTokenService service = at(ISSUED);

    assertThat(service.verify(null)).isEmpty();
    assertThat(service.verify("")).isEmpty();
    assertThat(service.verify("abc")).isEmpty();
    assertThat(service.verify("a.b")).isEmpty();
    assertThat(service.verify("!!.??.sig")).isEmpty();
    assertThat(service.verify("0.zzzzzz.sig")).isEmpty();
  }
}
