package inha.gdgoc.domain.club.schedule.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 소모임 QR 체크인 토큰. 행사 체크인({@code EventCheckinTokenService})과 같은 방식이다 — 테이블 없이 만료 시각과 서명만 들고 다닌다.
 *
 * <p>형식은 {@code <일정id>.<만료시각>.<서명>} 이고 서명이 일정 id 와 만료 시각을 함께 덮는다. 다른 일정 id 로 바꿔 적거나 시간을 늘리면
 * 서명이 맞지 않는다. 체크인 요청은 토큰만 보내므로 어느 일정인지도 토큰이 알려 준다.
 *
 * <p>서명 입력 앞에 {@code club-schedule:} 을 붙인다. 운영 실수로 행사와 같은 키를 넣어도 행사 토큰이 소모임 토큰으로 통하지 않는다.
 */
@Slf4j
@Component
public class ClubCheckinTokenService {

  /** 토큰 하나의 수명. 로그인을 거쳐 돌아오는 왕복을 감안해 행사와 같은 3분이다. */
  private static final long LIFETIME_SECONDS = 180;

  private static final int SIGNATURE_LENGTH = 12;
  private static final String SEPARATOR = ".";
  /** 숫자를 36진수로 적어 QR 에 들어가는 글자를 줄인다. */
  private static final int RADIX = 36;

  private final byte[] secret;
  private final Clock clock;

  @Autowired
  public ClubCheckinTokenService(@Value("${app.club-checkin.secret:}") String configuredSecret) {
    this(configuredSecret, Clock.system(ZoneId.of("Asia/Seoul")));
  }

  ClubCheckinTokenService(String configuredSecret, Clock clock) {
    this.clock = clock;
    if (configuredSecret == null || configuredSecret.isBlank()) {
      // 비워 두면 기동할 때마다 새로 만든다. 3분짜리 토큰이라 재시작으로 무효가 돼도 리더가 화면을
      // 새로고침하면 그만이다. 서버를 여러 대로 늘리면 반드시 app.club-checkin.secret 을 넣어야 한다.
      byte[] generated = new byte[32];
      new SecureRandom().nextBytes(generated);
      this.secret = generated;
      log.info("app.club-checkin.secret 이 없어 기동 시 임의 키를 생성했다. 단일 인스턴스에서만 유효하다.");
    } else {
      this.secret = configuredSecret.getBytes(StandardCharsets.UTF_8);
    }
  }

  /** 지금 화면에 띄울 토큰. 부를 때마다 만료 시각이 밀려 새 토큰이 나온다. */
  public String issue(Long scheduleId) {
    long expiresAt = Instant.now(clock).getEpochSecond() + LIFETIME_SECONDS;
    return Long.toString(scheduleId, RADIX)
        + SEPARATOR
        + Long.toString(expiresAt, RADIX)
        + SEPARATOR
        + sign(scheduleId, expiresAt);
  }

  public long lifetimeSeconds() {
    return LIFETIME_SECONDS;
  }

  /** 살아 있고 서명이 맞으면 그 일정 id. 아니면 빈 값. */
  public Optional<Long> verify(String token) {
    if (token == null || token.isBlank()) {
      return Optional.empty();
    }
    String[] parts = token.split("\\.", -1);
    if (parts.length != 3) {
      return Optional.empty();
    }

    long scheduleId;
    long expiresAt;
    try {
      scheduleId = Long.parseLong(parts[0], RADIX);
      expiresAt = Long.parseLong(parts[1], RADIX);
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
    if (scheduleId <= 0 || Instant.now(clock).getEpochSecond() > expiresAt) {
      return Optional.empty();
    }
    return matches(parts[2], sign(scheduleId, expiresAt))
        ? Optional.of(scheduleId)
        : Optional.empty();
  }

  private String sign(long scheduleId, long expiresAt) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      byte[] raw =
          mac.doFinal(
              ("club-schedule:" + scheduleId + ":" + expiresAt).getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(raw)
          .substring(0, SIGNATURE_LENGTH);
    } catch (Exception e) {
      // HmacSHA256 은 모든 JDK 에 있다. 여기 오면 런타임이 이상한 것이다.
      throw new IllegalStateException("체크인 토큰을 만들 수 없다", e);
    }
  }

  private boolean matches(String given, String expected) {
    return MessageDigest.isEqual(
        given.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
  }
}
