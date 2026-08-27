package inha.gdgoc.domain.eventapplication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import inha.gdgoc.domain.eventapplication.dto.response.CheckinResponse;
import inha.gdgoc.domain.eventapplication.entity.EventApplication;
import inha.gdgoc.domain.eventapplication.entity.EventApplicationForm;
import inha.gdgoc.domain.eventapplication.enums.EventAttendanceStatus;
import inha.gdgoc.domain.eventapplication.exception.EventApplicationErrorCode;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationFormRepository;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventCheckinServiceTest {

  private static final Long BOARD_ID = 10L;
  private static final Long FORM_ID = 100L;
  private static final Long USER_ID = 7L;

  /** 행사 기간(9/1~9/2) 안. KST 로 9월 1일 낮이다. */
  private static final Instant DURING_EVENT = Instant.parse("2026-09-01T03:00:00Z");

  @Mock private EventApplicationFormRepository formRepository;
  @Mock private EventApplicationRepository applicationRepository;

  @Test
  @DisplayName("신청자가 유효한 토큰으로 찍으면 참석으로 바뀐다")
  void checkInMarksAttended() {
    EventApplicationForm form = form();
    EventApplication application = application(form);
    givenForm(form);
    givenApplication(application);

    EventCheckinService service = service(DURING_EVENT);
    CheckinResponse result = service.checkIn(BOARD_ID, token(), USER_ID);

    assertThat(result.alreadyCheckedIn()).isFalse();
    assertThat(application.getAttendanceStatus()).isEqualTo(EventAttendanceStatus.ATTENDED);
    assertThat(application.getCheckedInAt()).isEqualTo(DURING_EVENT);
  }

  @Test
  @DisplayName("두 번 찍어도 오류가 아니라 이미 처리됨으로 알려준다")
  void secondScanIsNotAnError() {
    EventApplicationForm form = form();
    EventApplication application = application(form);
    Instant firstScan = DURING_EVENT.minusSeconds(600);
    application.checkIn(firstScan);
    givenForm(form);
    givenApplication(application);

    CheckinResponse result = service(DURING_EVENT).checkIn(BOARD_ID, token(), USER_ID);

    assertThat(result.alreadyCheckedIn()).isTrue();
    // 처음 찍은 시각을 덮어쓰지 않는다.
    assertThat(result.checkedInAt()).isEqualTo(firstScan);
    assertThat(application.getCheckedInAt()).isEqualTo(firstScan);
  }

  @Test
  @DisplayName("만료된 토큰을 거절한다")
  void rejectsExpiredToken() {
    EventApplicationForm form = form();
    givenForm(form);

    // 3분 전에 찍힌 QR 사진으로 시도하는 상황이다.
    String staleToken =
        new EventCheckinTokenService("s", Clock.fixed(DURING_EVENT.minusSeconds(180), ZoneOffset.UTC))
            .issue(FORM_ID);

    assertError(
        () -> service(DURING_EVENT).checkIn(BOARD_ID, staleToken, USER_ID),
        EventApplicationErrorCode.CHECKIN_TOKEN_INVALID);
  }

  @Test
  @DisplayName("행사 기간이 아니면 거절한다")
  void rejectsOutsideEventPeriod() {
    givenForm(form());

    Instant dayAfter = Instant.parse("2026-09-03T03:00:00Z");
    assertError(
        () -> service(dayAfter).checkIn(BOARD_ID, token(dayAfter), USER_ID),
        EventApplicationErrorCode.CHECKIN_NOT_IN_PERIOD);
  }

  @Test
  @DisplayName("신청하지 않은 사람은 신청부터 하라고 돌려보낸다")
  void rejectsWhenNotApplied() {
    givenForm(form());
    when(applicationRepository.findByFormIdAndUserId(FORM_ID, USER_ID)).thenReturn(Optional.empty());

    assertError(
        () -> service(DURING_EVENT).checkIn(BOARD_ID, token(), USER_ID),
        EventApplicationErrorCode.CHECKIN_NOT_APPLIED);
  }

  @Test
  @DisplayName("취소한 신청으로는 체크인할 수 없다")
  void rejectsCanceledApplication() {
    EventApplicationForm form = form();
    EventApplication application = application(form);
    application.cancel(DURING_EVENT.minusSeconds(60));
    givenForm(form);
    givenApplication(application);

    assertError(
        () -> service(DURING_EVENT).checkIn(BOARD_ID, token(), USER_ID),
        EventApplicationErrorCode.CHECKIN_NOT_APPLIED);
  }

  private EventCheckinService service(Instant now) {
    return new EventCheckinService(
        formRepository,
        applicationRepository,
        new EventCheckinTokenService("s", Clock.fixed(now, ZoneOffset.UTC)),
        Clock.fixed(now, ZoneOffset.UTC));
  }

  private static String token() {
    return token(DURING_EVENT);
  }

  private static String token(Instant now) {
    return new EventCheckinTokenService("s", Clock.fixed(now, ZoneOffset.UTC)).issue(FORM_ID);
  }

  private void givenForm(EventApplicationForm form) {
    when(formRepository.findByEventBoardId(BOARD_ID)).thenReturn(Optional.of(form));
  }

  private void givenApplication(EventApplication application) {
    when(applicationRepository.findByFormIdAndUserId(FORM_ID, USER_ID))
        .thenReturn(Optional.of(application));
  }

  private static EventApplicationForm form() {
    EventApplicationForm form =
        EventApplicationForm.create(
            BOARD_ID,
            "가을 해커톤",
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 2),
            null,
            null,
            null,
            UserRole.MEMBER,
            true);
    ReflectionTestUtils.setField(form, "id", FORM_ID);
    return form;
  }

  private static EventApplication application(EventApplicationForm form) {
    User user = User.builder().name("홍길동").build();
    ReflectionTestUtils.setField(user, "id", USER_ID);
    return EventApplication.create(form, user, DURING_EVENT.minusSeconds(86400));
  }

  private void assertError(Runnable action, EventApplicationErrorCode expected) {
    assertThatThrownBy(action::run)
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(expected);
  }
}
