package inha.gdgoc.domain.homecoming.service;

import inha.gdgoc.domain.homecoming.entity.HomecomingApplication;
import inha.gdgoc.domain.homecoming.entity.HomecomingConsent;
import inha.gdgoc.domain.homecoming.enums.ConsentPurpose;
import inha.gdgoc.domain.homecoming.enums.HomecomingStatus;
import inha.gdgoc.domain.homecoming.exception.HomecomingErrorCode;
import inha.gdgoc.domain.homecoming.repository.HomecomingApplicationRepository;
import inha.gdgoc.domain.homecoming.repository.HomecomingConsentRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신청 저장의 트랜잭션 경계. {@link HomecomingApplicationService} 와 빈을 나눈 이유는, 동시 요청이 유니크 제약에 걸렸을 때 이
 * 트랜잭션은 이미 망가져 있어서 바깥에서 새 트랜잭션으로 다시 조회해야 하기 때문이다.
 */
@Component
@RequiredArgsConstructor
class HomecomingApplicationWriter {

  private final HomecomingApplicationRepository applicationRepository;
  private final HomecomingConsentRepository consentRepository;

  @Transactional
  public HomecomingApplication insert(
      String requestId,
      HomecomingProfile profile,
      ConsentDecision consents,
      Instant now,
      Instant optionalConsentExpiresAt) {
    if (applicationRepository.existsByPhoneAndStatus(profile.phone(), HomecomingStatus.ACTIVE)) {
      throw new BusinessException(HomecomingErrorCode.DUPLICATE_PHONE);
    }
    HomecomingApplication application =
        applicationRepository.saveAndFlush(HomecomingApplication.submit(requestId, profile, now));

    String version = consents.noticeVersion();
    consentRepository.saveAll(
        List.of(
            HomecomingConsent.record(application, ConsentPurpose.PRIVACY, true, version, now, null),
            HomecomingConsent.record(
                application,
                ConsentPurpose.EVENT_INVITE,
                consents.eventInvite(),
                version,
                now,
                optionalConsentExpiresAt),
            HomecomingConsent.record(
                application,
                ConsentPurpose.TALENT_POOL,
                consents.talentPool(),
                version,
                now,
                optionalConsentExpiresAt)));
    return application;
  }

  record ConsentDecision(boolean eventInvite, boolean talentPool, String noticeVersion) {}
}
