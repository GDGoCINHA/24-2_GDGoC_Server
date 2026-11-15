package inha.gdgoc.domain.homecoming.service;

import static inha.gdgoc.domain.homecoming.exception.HomecomingErrorCode.*;

import inha.gdgoc.domain.homecoming.dto.request.HomecomingUpdateRequest;
import inha.gdgoc.domain.homecoming.dto.response.HomecomingApplicationResponse;
import inha.gdgoc.domain.homecoming.dto.response.HomecomingConsentEntryResponse;
import inha.gdgoc.domain.homecoming.dto.response.HomecomingSummaryResponse;
import inha.gdgoc.domain.homecoming.entity.HomecomingApplication;
import inha.gdgoc.domain.homecoming.entity.HomecomingConsent;
import inha.gdgoc.domain.homecoming.enums.AttendType;
import inha.gdgoc.domain.homecoming.enums.ConsentPurpose;
import inha.gdgoc.domain.homecoming.enums.ConsentState;
import inha.gdgoc.domain.homecoming.enums.HomecomingStatus;
import inha.gdgoc.domain.homecoming.repository.HomecomingApplicationRepository;
import inha.gdgoc.domain.homecoming.repository.HomecomingConsentRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회장·HR 의 신청 관리. 신청자는 직접 고치거나 취소할 수 없으므로 변경 요청은 모두 여기로 온다.
 *
 * <p>신청은 수백 건 규모라 목록·집계를 메모리에서 거른다. 쿼리를 나누는 것보다 규칙(취소 제외·유형별 분류)이 한눈에 보인다.
 */
@Service
@Transactional(readOnly = true)
public class HomecomingAdminService {

  private final HomecomingApplicationRepository applicationRepository;
  private final HomecomingConsentRepository consentRepository;
  private final HomecomingProfileNormalizer normalizer;
  private final HomecomingCsvWriter csvWriter;
  private final Clock clock;

  @Autowired
  public HomecomingAdminService(
      HomecomingApplicationRepository applicationRepository,
      HomecomingConsentRepository consentRepository,
      HomecomingProfileNormalizer normalizer,
      HomecomingCsvWriter csvWriter) {
    this(
        applicationRepository,
        consentRepository,
        normalizer,
        csvWriter,
        Clock.system(ZoneId.of("Asia/Seoul")));
  }

  /** 테스트가 동의 만료 판정 시각을 고정할 때 쓴다. */
  HomecomingAdminService(
      HomecomingApplicationRepository applicationRepository,
      HomecomingConsentRepository consentRepository,
      HomecomingProfileNormalizer normalizer,
      HomecomingCsvWriter csvWriter,
      Clock clock) {
    this.applicationRepository = applicationRepository;
    this.consentRepository = consentRepository;
    this.normalizer = normalizer;
    this.csvWriter = csvWriter;
    this.clock = clock;
  }

  public List<HomecomingApplicationResponse> list(HomecomingStatus status, AttendType attendType) {
    List<HomecomingApplication> rows =
        applicationRepository.findAllByOrderBySubmittedAtAsc().stream()
            .filter(a -> status == null || a.getStatus() == status)
            .filter(a -> attendType == null || a.getAttendType() == attendType)
            .toList();
    Map<Long, List<HomecomingConsent>> consents =
        consentRepository
            .findAllByApplicationIdIn(rows.stream().map(HomecomingApplication::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(c -> c.getApplication().getId()));
    Instant now = clock.instant();
    return rows.stream()
        .map(a -> toResponse(a, consents.getOrDefault(a.getId(), List.of()), now))
        .toList();
  }

  public HomecomingApplicationResponse get(Long id) {
    HomecomingApplication a = find(id);
    return toResponse(a, consentRepository.findAllByApplicationId(id), clock.instant());
  }

  @Transactional
  public HomecomingApplicationResponse update(Long id, HomecomingUpdateRequest req, Long adminId) {
    HomecomingApplication a = find(id);
    if (!a.isActive()) throw new BusinessException(APPLICATION_CANCELED);
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
    if (applicationRepository.existsByPhoneAndStatusAndIdNot(
        profile.phone(), HomecomingStatus.ACTIVE, id)) {
      throw new BusinessException(DUPLICATE_PHONE);
    }
    a.edit(profile, adminId);
    return toResponse(a, consentRepository.findAllByApplicationId(id), clock.instant());
  }

  /** 이미 취소된 신청을 다시 취소해도 그대로 둔다(첫 취소 시각·사유 유지). */
  @Transactional
  public void cancel(Long id, String reason, Long adminId) {
    HomecomingApplication a = find(id);
    if (!a.isActive()) return;
    a.cancel(reason == null || reason.isBlank() ? null : reason.strip(), adminId, clock.instant());
  }

  public HomecomingSummaryResponse summary() {
    List<HomecomingApplication> all = applicationRepository.findAll();
    List<HomecomingApplication> active = all.stream().filter(HomecomingApplication::isActive).toList();
    Map<AttendType, Long> by =
        active.stream().collect(Collectors.groupingBy(HomecomingApplication::getAttendType, Collectors.counting()));
    return new HomecomingSummaryResponse(
        by.getOrDefault(AttendType.MAIN, 0L),
        by.getOrDefault(AttendType.AFTER, 0L),
        by.getOrDefault(AttendType.BOTH, 0L),
        by.getOrDefault(AttendType.MAYBE, 0L),
        by.getOrDefault(AttendType.NO, 0L),
        active.size(),
        active.stream().filter(a -> a.getAttendType().attendsMain()).count(),
        active.stream().filter(a -> a.getAttendType().attendsAfterParty()).count(),
        active.stream().filter(HomecomingApplication::isLate).count(),
        all.size() - active.size());
  }

  /** 본 행사 참석자(main·both)의 명찰 자료. 시작 연도 → 이름 순. */
  public byte[] exportNametags() {
    List<HomecomingApplication> rows =
        applicationRepository.findAll().stream()
            .filter(HomecomingApplication::isActive)
            .filter(a -> a.getAttendType().attendsMain())
            .sorted(
                Comparator.comparing(HomecomingApplication::getYearStart)
                    .thenComparing(HomecomingApplication::getName))
            .toList();
    return csvWriter.nametags(rows);
  }

  public byte[] exportApplications() {
    return csvWriter.applications(applicationRepository.findAllByOrderBySubmittedAtAsc());
  }

  /**
   * 장기 등록 목록. state 를 비우면 전부, EXPIRED·WITHDRAWN 이면 삭제 대상이다.
   *
   * <p>필수 동의(PRIVACY)는 장기 등록이 아니므로 받지 않는다.
   */
  public List<HomecomingConsentEntryResponse> consents(ConsentPurpose purpose, ConsentState state) {
    if (!purpose.isLongTerm()) throw new BusinessException(CONSENT_NOT_LONG_TERM);
    Instant now = clock.instant();
    return consentRepository.findAllByPurposeOrderByDecidedAtAsc(purpose).stream()
        .filter(c -> state == null ? c.isAgreed() : c.stateAt(now) == state)
        .map(c -> HomecomingConsentEntryResponse.of(c, now))
        .toList();
  }

  /** 신청자가 HR 에게 철회를 요청하면 담당자가 기록한다. 이미 철회된 건은 그대로 둔다. */
  @Transactional
  public void withdrawConsent(Long applicationId, ConsentPurpose purpose, Long adminId) {
    if (!purpose.isLongTerm()) throw new BusinessException(CONSENT_NOT_LONG_TERM);
    find(applicationId);
    HomecomingConsent c =
        consentRepository
            .findByApplicationIdAndPurpose(applicationId, purpose)
            .orElseThrow(() -> new BusinessException(APPLICATION_NOT_FOUND));
    if (!c.isAgreed()) throw new BusinessException(CONSENT_NOT_AGREED);
    if (c.getWithdrawnAt() == null) c.withdraw(adminId, clock.instant());
  }

  private HomecomingApplication find(Long id) {
    return applicationRepository
        .findById(id)
        .orElseThrow(() -> new BusinessException(APPLICATION_NOT_FOUND));
  }

  private static HomecomingApplicationResponse toResponse(
      HomecomingApplication a, List<HomecomingConsent> consents, Instant now) {
    return HomecomingApplicationResponse.of(
        a, stateOf(consents, ConsentPurpose.EVENT_INVITE, now), stateOf(consents, ConsentPurpose.TALENT_POOL, now));
  }

  private static ConsentState stateOf(List<HomecomingConsent> consents, ConsentPurpose purpose, Instant now) {
    return consents.stream()
        .filter(c -> c.getPurpose() == purpose)
        .findFirst()
        .map(c -> c.stateAt(now))
        .orElse(ConsentState.REFUSED);
  }
}
