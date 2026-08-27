package inha.gdgoc.domain.eventapplication.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * QR 체크인 토큰.
 *
 * <p>행사장에 띄운 QR 을 찍어서 안 온 사람에게 보내는 것이 이 방식의 유일한 약점이다. 토큰을 주기적으로 갈아치우면 사진으로 받은 QR 은 곧 무효가 된다.
 *
 * <p><b>토큰이 자기 만료 시각을 들고 다닌다.</b> {@code <만료시각>.<서명>} 형태이고 서명이 만료 시각까지 덮으므로 시간을 늘려 적을 수 없다. 저장
 * 테이블도, 서버 메모리 상태도 필요 없다.
 *
 * <p>예전에는 {@code HMAC(비밀키, 폼ID + 벽시계창번호)} 였다. 창 번호가 벽시계에서 나오는 탓에 <b>같은 창 안에서는 몇 번을 다시 발급해도 같은
 * 토큰이 나왔고</b>, 남은 시간도 "발급 후 경과" 가 아니라 다음 경계까지의 거리였다. 관리자 화면의 「QR 새로 받기」 가 눌러도 아무것도 안 바뀌는 버튼이
 * 됐던 이유다. 지금은 발급할 때마다 새 토큰이 나오고 수명이 항상 정확히 {@link #LIFETIME_SECONDS} 초다.
 */
@Slf4j
@Component
public class EventCheckinTokenService {

  /**
   * 토큰 하나의 수명.
   *
   * <p>QR 을 찍은 사람이 로그인되어 있지 않으면 구글 로그인을 거쳐 돌아온다. 60 초로는 그 왕복이 자주 넘쳐 입구에서 두 번 찍게 만들었다. 늘려서 잃는
   * 것은 QR 사진을 전달할 여유인데, 그건 애초에 토큰 수명으로 막을 수 없다 — 실질적인 억제는 운영진이 체크인 명단을 본다는 데서 온다.
   */
  private static final long LIFETIME_SECONDS = 180;

  private static final int SIGNATURE_LENGTH = 10;
  private static final char SEPARATOR = '.';
  /** 만료 시각을 36진수로 적어 QR 에 들어가는 글자를 줄인다. */
  private static final int EXPIRY_RADIX = 36;

  private final byte[] secret;
  private final Clock clock;

  @Autowired
  public EventCheckinTokenService(@Value("${app.event-checkin.secret:}") String configuredSecret) {
    this(configuredSecret, Clock.system(ZoneId.of("Asia/Seoul")));
  }

  EventCheckinTokenService(String configuredSecret, Clock clock) {
    this.clock = clock;
    if (configuredSecret == null || configuredSecret.isBlank()) {
      // 설정이 없으면 기동할 때마다 새로 만든다. 3 분짜리 토큰이라 재시작으로 무효가 되어도
      // 화면을 새로고침하면 그만이다. 다만 서버를 여러 대로 늘리면 인스턴스마다 키가 달라져
      // 검증이 실패하므로, 그때는 app.event-checkin.secret 을 반드시 넣어야 한다.
      byte[] generated = new byte[32];
      new SecureRandom().nextBytes(generated);
      this.secret = generated;
      log.info("app.event-checkin.secret 이 없어 기동 시 임의 키를 생성했다. 단일 인스턴스에서만 유효하다.");
    } else {
      this.secret = configuredSecret.getBytes(StandardCharsets.UTF_8);
    }
  }

  /** 지금 화면에 띄울 토큰. 부를 때마다 만료 시각이 밀려 새 토큰이 나온다. */
  public String issue(Long formId) {
    long expiresAt = Instant.now(clock).getEpochSecond() + LIFETIME_SECONDS;
    return Long.toString(expiresAt, EXPIRY_RADIX) + SEPARATOR + sign(formId, expiresAt);
  }

  /** 방금 발급한 토큰이 살아 있는 시간. 관리자 화면이 카운트다운에 쓴다. */
  public long lifetimeSeconds() {
    return LIFETIME_SECONDS;
  }

  public boolean verify(Long formId, String token) {
    if (token == null || token.isBlank()) {
      return false;
    }
    int separator = token.indexOf(SEPARATOR);
    if (separator <= 0 || separator == token.length() - 1) {
      return false;
    }

    long expiresAt;
    try {
      expiresAt = Long.parseLong(token.substring(0, separator), EXPIRY_RADIX);
    } catch (NumberFormatException e) {
      return false;
    }
    if (Instant.now(clock).getEpochSecond() > expiresAt) {
      return false;
    }

    // 서명을 만료 시각까지 덮으므로 시간을 늘려 적으면 서명이 안 맞는다.
    return matches(token.substring(separator + 1), sign(formId, expiresAt));
  }

  private String sign(Long formId, long expiresAt) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      byte[] raw = mac.doFinal((formId + ":" + expiresAt).getBytes(StandardCharsets.UTF_8));
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
