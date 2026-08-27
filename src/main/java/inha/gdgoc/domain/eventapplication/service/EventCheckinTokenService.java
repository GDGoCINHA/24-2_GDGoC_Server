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
 * <p>행사장에 띄운 QR 을 찍어서 안 온 사람에게 보내는 것이 이 방식의 유일한 약점이다. 토큰을 {@code HMAC(비밀키, 폼ID + 창 번호)} 로 계산해 주기적으로
 * 갈아치우면 사진으로 받은 QR 은 무효가 된다. 저장 테이블이 필요 없다.
 *
 * <p>검증할 때 현재 창과 직전 창을 모두 받아준다. 창이 끝나갈 때 찍은 사람이 경계를 넘었다고 실패하면 안 되기 때문이다. 그래서 실제 유효 시간은 찍은 시점에
 * 따라 {@link #WINDOW_SECONDS} 의 1~2 배다.
 *
 * <p><b>창을 60 초에서 180 초로 늘렸다(2026-08-27).</b> QR 을 찍은 사람이 로그인되어 있지 않으면 구글 로그인을 거쳐 돌아오는데, 60 초로는 그 왕복이
 * 자주 넘쳤다. 만료되면 안내가 뜨고 다시 찍으면 되지만 입구에서 두 번 찍게 만든다. 늘려서 잃는 것은 QR 사진을 전달할 여유가 몇 분 늘어나는 것인데, 그건
 * 애초에 토큰 수명으로 막을 수 없다 — 실질적인 억제는 운영진이 체크인 명단을 본다는 데서 온다.
 */
@Slf4j
@Component
public class EventCheckinTokenService {

  private static final long WINDOW_SECONDS = 180;
  private static final int TOKEN_LENGTH = 10;

  private final byte[] secret;
  private final Clock clock;

  @Autowired
  public EventCheckinTokenService(@Value("${app.event-checkin.secret:}") String configuredSecret) {
    this(configuredSecret, Clock.system(ZoneId.of("Asia/Seoul")));
  }

  EventCheckinTokenService(String configuredSecret, Clock clock) {
    this.clock = clock;
    if (configuredSecret == null || configuredSecret.isBlank()) {
      // 설정이 없으면 기동할 때마다 새로 만든다. 60 초짜리 토큰이라 재시작으로 무효가 되어도
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

  /** 지금 화면에 띄울 토큰. */
  public String issue(Long formId) {
    return sign(formId, currentWindow());
  }

  /** 이 토큰이 바뀌기까지 남은 초. 관리자 화면이 카운트다운에 쓴다. */
  public long remainingSeconds() {
    long epochSecond = Instant.now(clock).getEpochSecond();
    return WINDOW_SECONDS - (epochSecond % WINDOW_SECONDS);
  }

  public boolean verify(Long formId, String token) {
    if (token == null || token.isBlank()) {
      return false;
    }
    long window = currentWindow();
    return matches(token, sign(formId, window)) || matches(token, sign(formId, window - 1));
  }

  private long currentWindow() {
    return Instant.now(clock).getEpochSecond() / WINDOW_SECONDS;
  }

  private String sign(Long formId, long window) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      byte[] raw = mac.doFinal((formId + ":" + window).getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(raw).substring(0, TOKEN_LENGTH);
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
