package inha.gdgoc.domain.eventapplication.service;

import static inha.gdgoc.domain.eventapplication.exception.EventApplicationErrorCode.*;

import inha.gdgoc.domain.eventapplication.dto.response.CheckinResponse;
import inha.gdgoc.domain.eventapplication.dto.response.CheckinTokenResponse;
import inha.gdgoc.domain.eventapplication.entity.EventApplication;
import inha.gdgoc.domain.eventapplication.entity.EventApplicationForm;
import inha.gdgoc.domain.eventapplication.enums.ApplicationStatus;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationFormRepository;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * QR 체크인.
 *
 * <p>운영진이 행사장에 QR 을 띄우고 부원이 각자 스캔한다. 부원의 기본 카메라 앱이 브라우저를 열어주므로 서버도 웹도 카메라를 다루지 않는다.
 *
 * <p>수기 참석 처리는 그대로 남겨둔다. 폰이 없거나 행사장 네트워크가 안 되는 사람이 있고, QR 이 안 될 때 행사가 멈추면 안 되기 때문이다.
 *
 * <p>로그인하지 않은 폰은 학번·이름으로 신청을 찾는다({@link #checkInAnonymously}). QR 토큰이 행사장에서만 유효하므로 계정 체크인보다 크게
 * 느슨하지 않다.
 */
@Service
@Transactional(readOnly = true)
public class EventCheckinService {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final EventApplicationFormRepository formRepository;
  private final EventApplicationRepository applicationRepository;
  private final UserRepository userRepository;
  private final EventCheckinTokenService tokenService;
  private final Clock clock;

  @Autowired
  public EventCheckinService(
      EventApplicationFormRepository formRepository,
      EventApplicationRepository applicationRepository,
      UserRepository userRepository,
      EventCheckinTokenService tokenService) {
    this(formRepository, applicationRepository, userRepository, tokenService, Clock.system(KST));
  }

  EventCheckinService(
      EventApplicationFormRepository formRepository,
      EventApplicationRepository applicationRepository,
      UserRepository userRepository,
      EventCheckinTokenService tokenService,
      Clock clock) {
    this.formRepository = formRepository;
    this.applicationRepository = applicationRepository;
    this.userRepository = userRepository;
    this.tokenService = tokenService;
    this.clock = clock;
  }

  /** 관리자 화면이 주기적으로 불러 QR 을 새로 그린다. */
  public CheckinTokenResponse issueToken(Long eventBoardId) {
    EventApplicationForm form = findForm(eventBoardId);
    return new CheckinTokenResponse(
        eventBoardId, tokenService.issue(form.getId()), tokenService.lifetimeSeconds());
  }

  @Transactional
  public CheckinResponse checkIn(Long eventBoardId, String token, Long userId) {
    EventApplicationForm form = findForm(eventBoardId);
    Instant now = Instant.now(clock);
    verify(form, token, now);

    Optional<EventApplication> own =
        applicationRepository
            .findByFormIdAndUserId(form.getId(), userId)
            .filter(EventApplication::isApplied);
    // 링크로 로그인 없이 신청해 두고 나중에 로그인한 사람이다. 계정의 학번·이름으로 그 신청을 찾는다.
    EventApplication application =
        own.or(() -> findAnonymousOf(form, userId))
            .orElseThrow(() -> new BusinessException(CHECKIN_NOT_APPLIED));

    return markCheckedIn(form, application, now);
  }

  @Transactional
  public CheckinResponse checkInAnonymously(
      Long eventBoardId, String token, String studentId, String name) {
    EventApplicationForm form = findForm(eventBoardId);
    Instant now = Instant.now(clock);
    verify(form, token, now);

    EventApplication application =
        applicationRepository
            .findByFormIdAndStudentId(form.getId(), studentId.trim(), ApplicationStatus.APPLIED)
            .stream()
            // 학번만 보면 오타 하나로 남의 신청에 체크인된다. 이름까지 맞아야 인정한다.
            .filter(candidate -> sameName(candidate.resolvedName(), name))
            .findFirst()
            .orElseThrow(() -> new BusinessException(CHECKIN_IDENTITY_NOT_FOUND));

    return markCheckedIn(form, application, now);
  }

  private Optional<EventApplication> findAnonymousOf(EventApplicationForm form, Long userId) {
    User user = userRepository.findById(userId).orElse(null);
    if (user == null) {
      return Optional.empty();
    }
    return applicationRepository
        .findByFormIdAndStudentId(form.getId(), user.getStudentId(), ApplicationStatus.APPLIED)
        .stream()
        .filter(candidate -> candidate.getUser() == null)
        .filter(candidate -> sameName(candidate.getApplicantName(), user.getName()))
        .findFirst();
  }

  private void verify(EventApplicationForm form, String token, Instant now) {
    if (!withinEventPeriod(form, now)) {
      throw new BusinessException(CHECKIN_NOT_IN_PERIOD);
    }
    if (!tokenService.verify(form.getId(), token)) {
      throw new BusinessException(CHECKIN_TOKEN_INVALID);
    }
  }

  private CheckinResponse markCheckedIn(
      EventApplicationForm form, EventApplication application, Instant now) {
    if (application.getCheckedInAt() != null) {
      // 두 번 찍는 것은 흔한 일이다. 오류로 다루지 않는다.
      return new CheckinResponse(true, application.getCheckedInAt(), form.getEventTitle());
    }

    application.checkIn(now);
    return new CheckinResponse(false, now, form.getEventTitle());
  }

  /** 띄어쓰기는 사람마다 다르게 적는다("홍 길동"). 그 차이로 체크인이 막히면 안 된다. */
  private boolean sameName(String a, String b) {
    if (a == null || b == null) {
      return false;
    }
    return a.replaceAll("\\s+", "").equals(b.replaceAll("\\s+", ""));
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
