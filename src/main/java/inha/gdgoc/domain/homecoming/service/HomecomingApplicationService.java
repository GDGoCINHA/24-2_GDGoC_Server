package inha.gdgoc.domain.homecoming.service;

import inha.gdgoc.domain.homecoming.dto.request.HomecomingApplyRequest;
import inha.gdgoc.domain.homecoming.dto.response.HomecomingApplyResponse;
import inha.gdgoc.domain.homecoming.exception.HomecomingErrorCode;
import inha.gdgoc.domain.homecoming.repository.HomecomingApplicationRepository;
import inha.gdgoc.domain.homecoming.service.HomecomingApplicationWriter.ConsentDecision;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 로그인 없이 받는 홈커밍데이 신청.
 *
 * <p>순서가 중요하다. 같은 {@code requestId} 가 이미 있으면 마감·중복 검사보다 먼저 그 결과를 돌려준다 — 마감 직전에 저장된 뒤 응답만
 * 유실된 신청이 재시도에서 "마감"이나 "이미 신청"으로 보이면 안 된다.
 *
 * <p>이 클래스에는 트랜잭션을 걸지 않는다. 저장은 {@link HomecomingApplicationWriter} 가 자기 트랜잭션에서 하고, 유니크 제약 충돌은 여기서
 * 새로 조회해 해석한다.
 */
@Service
public class HomecomingApplicationService {

  /** 선택 동의(행사 초청·인재풀) 보관·이용 기한. 안내문에 적힌 날짜와 같아야 한다. */
  static final Instant OPTIONAL_CONSENT_EXPIRES_AT =
      OffsetDateTime.parse("2027-12-31T23:59:59.999+09:00").toInstant();

  private final HomecomingApplicationRepository applicationRepository;
  private final HomecomingApplicationWriter writer;
  private final HomecomingProfileNormalizer normalizer;
  private final Instant closeAt;
  private final Clock clock;

  @Autowired
  public HomecomingApplicationService(
      HomecomingApplicationRepository applicationRepository,
      HomecomingApplicationWriter writer,
      HomecomingProfileNormalizer normalizer,
      // 정확한 마감 시각은 운영진 확인 전이다. 바뀌면 설정(app.homecoming.close-at)만 고친다.
      @Value("${app.homecoming.close-at:2026-11-22T23:59:59+09:00}") OffsetDateTime closeAt) {
    this(
        applicationRepository,
        writer,
        normalizer,
        closeAt.toInstant(),
        Clock.system(ZoneId.of("Asia/Seoul")));
  }

  /** 테스트가 마감 시각과 현재 시각을 고정할 때 쓴다. */
  HomecomingApplicationService(
      HomecomingApplicationRepository applicationRepository,
      HomecomingApplicationWriter writer,
      HomecomingProfileNormalizer normalizer,
      Instant closeAt,
      Clock clock) {
    this.applicationRepository = applicationRepository;
    this.writer = writer;
    this.normalizer = normalizer;
    this.closeAt = closeAt;
    this.clock = clock;
  }

  public HomecomingApplyResponse submit(HomecomingApplyRequest req) {
    Optional<HomecomingApplyResponse> replay = findByRequestId(req.requestId());
    if (replay.isPresent()) return replay.get();

    Instant now = clock.instant();
    if (now.isAfter(closeAt)) throw new BusinessException(HomecomingErrorCode.CLOSED);
    if (!req.privacyAgreed()) throw new BusinessException(HomecomingErrorCode.PRIVACY_REQUIRED);

    HomecomingProfile profile =
        normalizer.normalize(
            req.attendType(),
            req.name(),
            req.phone(),
            req.yearStart(),
            req.yearEnd(),
            req.memberRole(),
            req.team(),
            req.isLate(),
            req.arrivalTime(),
            req.message());
    ConsentDecision consents =
        new ConsentDecision(req.eventInviteAgreed(), req.talentPoolAgreed(), req.consentVersion());

    try {
      return HomecomingApplyResponse.from(
          writer.insert(req.requestId(), profile, consents, now, OPTIONAL_CONSENT_EXPIRES_AT));
    } catch (DataIntegrityViolationException e) {
      // 같은 requestId 재시도가 동시에 들어왔거나, 같은 번호가 동시에 들어왔다.
      return findByRequestId(req.requestId())
          .orElseThrow(() -> new BusinessException(HomecomingErrorCode.DUPLICATE_PHONE));
    }
  }

  public boolean isClosed() {
    return clock.instant().isAfter(closeAt);
  }

  public Instant closeAt() {
    return closeAt;
  }

  private Optional<HomecomingApplyResponse> findByRequestId(String requestId) {
    return applicationRepository.findByRequestId(requestId).map(HomecomingApplyResponse::from);
  }
}
