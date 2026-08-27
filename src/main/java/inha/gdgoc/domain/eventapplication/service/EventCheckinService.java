package inha.gdgoc.domain.eventapplication.service;

import static inha.gdgoc.domain.eventapplication.exception.EventApplicationErrorCode.*;

import inha.gdgoc.domain.eventapplication.dto.response.CheckinResponse;
import inha.gdgoc.domain.eventapplication.dto.response.CheckinTokenResponse;
import inha.gdgoc.domain.eventapplication.entity.EventApplication;
import inha.gdgoc.domain.eventapplication.entity.EventApplicationForm;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationFormRepository;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * QR 체크인.
 *
 * <p>운영진이 행사장에 QR 을 띄우고 부원이 각자 스캔한다. 부원의 기본 카메라 앱이 브라우저를 열어주므로 서버도 웹도 카메라를 다루지 않는다.
 *
 * <p>수기 참석 처리는 그대로 남겨둔다. 폰이 없거나 행사장 네트워크가 안 되는 사람이 있고, QR 이 안 될 때 행사가 멈추면 안 되기 때문이다.
 */
@Service
@Transactional(readOnly = true)
public class EventCheckinService {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final EventApplicationFormRepository formRepository;
  private final EventApplicationRepository applicationRepository;
  private final EventCheckinTokenService tokenService;
  private final Clock clock;

  @Autowired
  public EventCheckinService(
      EventApplicationFormRepository formRepository,
      EventApplicationRepository applicationRepository,
      EventCheckinTokenService tokenService) {
    this(formRepository, applicationRepository, tokenService, Clock.system(KST));
  }

  EventCheckinService(
      EventApplicationFormRepository formRepository,
      EventApplicationRepository applicationRepository,
      EventCheckinTokenService tokenService,
      Clock clock) {
    this.formRepository = formRepository;
    this.applicationRepository = applicationRepository;
    this.tokenService = tokenService;
    this.clock = clock;
  }

  /** 관리자 화면이 주기적으로 불러 QR 을 새로 그린다. */
  public CheckinTokenResponse issueToken(Long eventBoardId) {
    EventApplicationForm form = findForm(eventBoardId);
    return new CheckinTokenResponse(
        eventBoardId, tokenService.issue(form.getId()), tokenService.remainingSeconds());
  }

  @Transactional
  public CheckinResponse checkIn(Long eventBoardId, String token, Long userId) {
    EventApplicationForm form = findForm(eventBoardId);

    Instant now = Instant.now(clock);
    if (!withinEventPeriod(form, now)) {
      throw new BusinessException(CHECKIN_NOT_IN_PERIOD);
    }
    if (!tokenService.verify(form.getId(), token)) {
      throw new BusinessException(CHECKIN_TOKEN_INVALID);
    }

    EventApplication application =
        applicationRepository
            .findByFormIdAndUserId(form.getId(), userId)
            .filter(EventApplication::isApplied)
            .orElseThrow(() -> new BusinessException(CHECKIN_NOT_APPLIED));

    if (application.getCheckedInAt() != null) {
      // 두 번 찍는 것은 흔한 일이다. 오류로 다루지 않는다.
      return new CheckinResponse(true, application.getCheckedInAt(), form.getEventTitle());
    }

    application.checkIn(now);
    return new CheckinResponse(false, now, form.getEventTitle());
  }

  /** 행사 시작일 00:00 부터 종료일 24:00 까지. 별도 설정을 두지 않고 행사 날짜를 그대로 쓴다. */
  private boolean withinEventPeriod(EventApplicationForm form, Instant now) {
    LocalDate today = now.atZone(KST).toLocalDate();
    return !today.isBefore(form.getEventStartDate()) && !today.isAfter(form.getEventEndDate());
  }

  private EventApplicationForm findForm(Long eventBoardId) {
    return formRepository
        .findByEventBoardId(eventBoardId)
        .orElseThrow(() -> new BusinessException(FORM_NOT_FOUND));
  }
}
