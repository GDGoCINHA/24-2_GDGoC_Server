package inha.gdgoc.domain.eventapplication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import inha.gdgoc.global.util.MajorNormalizer;
import inha.gdgoc.domain.eventapplication.dto.request.AttendanceUpdateRequest;
import inha.gdgoc.domain.eventapplication.dto.request.ProxyApplicationRequest;
import inha.gdgoc.domain.eventapplication.entity.EventApplication;
import inha.gdgoc.domain.eventapplication.entity.EventApplicationForm;
import inha.gdgoc.domain.eventapplication.enums.ApplicationStatus;
import inha.gdgoc.domain.eventapplication.enums.EventAttendanceStatus;
import inha.gdgoc.domain.eventapplication.exception.EventApplicationErrorCode;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationFormRepository;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationRepository;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.domain.user.repository.UserRepository;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventApplicantAdminServiceTest {

  private static final Long BOARD_ID = 10L;
  private static final Long FORM_ID = 100L;
  private static final Long USER_ID = 7L;
  private static final Instant NOW = Instant.parse("2026-09-01T03:00:00Z");

  @Mock private EventApplicationFormRepository formRepository;
  @Mock private EventApplicationRepository applicationRepository;
  @Mock private UserRepository userRepository;

  private EventApplicantAdminService service;

  @BeforeEach
  void setUp() {
    service =
        new EventApplicantAdminService(
            formRepository,
            applicationRepository,
            userRepository,
            new AnswerCodec(new ObjectMapper()),
            new ApplicantCsvWriter(new MajorNormalizer()),
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  @DisplayName("다른 행사의 신청 id 로는 참석을 바꿀 수 없다")
  void cannotTouchApplicationOfAnotherEvent() {
    givenForm(form(FORM_ID));
    EventApplication otherEvent = EventApplication.create(form(999L), user(), NOW);
    ReflectionTestUtils.setField(otherEvent, "id", 55L);
    when(applicationRepository.findById(55L)).thenReturn(Optional.of(otherEvent));

    // 경로의 행사와 신청이 맞물리는지 보지 않으면 남의 행사 참석을 건드릴 수 있다.
    assertError(
        () ->
            service.updateAttendance(
                BOARD_ID, 55L, new AttendanceUpdateRequest(EventAttendanceStatus.ATTENDED)),
        EventApplicationErrorCode.APPLICATION_NOT_FOUND);
    assertThat(otherEvent.getAttendanceStatus()).isEqualTo(EventAttendanceStatus.PENDING);
  }

  @Test
  @DisplayName("수기 참석 처리는 QR 체크인 시각을 남기지 않는다")
  void manualAttendanceLeavesCheckedInAtNull() {
    EventApplicationForm form = form(FORM_ID);
    givenForm(form);
    EventApplication application = EventApplication.create(form, user(), NOW);
    ReflectionTestUtils.setField(application, "id", 1L);
    when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));

    service.updateAttendance(
        BOARD_ID, 1L, new AttendanceUpdateRequest(EventAttendanceStatus.ATTENDED));

    assertThat(application.getAttendanceStatus()).isEqualTo(EventAttendanceStatus.ATTENDED);
    // 값이 비어 있어야 나중에 QR 로 온 사람과 수기 처리를 구분할 수 있다.
    assertThat(application.getCheckedInAt()).isNull();
  }

  @Test
  @DisplayName("현장 참석자를 등록하면 신청과 참석이 함께 생긴다")
  void proxyRegistrationCreatesAttendedApplication() {
    givenForm(form(FORM_ID));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
    when(applicationRepository.findByFormIdAndUserId(FORM_ID, USER_ID)).thenReturn(Optional.empty());

    service.registerProxy(BOARD_ID, new ProxyApplicationRequest(USER_ID, true));

    verify(applicationRepository).save(any());
  }

  @Test
  @DisplayName("이미 신청한 사람은 대리 등록할 수 없다")
  void proxyRejectsExistingApplicant() {
    EventApplicationForm form = form(FORM_ID);
    givenForm(form);
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
    when(applicationRepository.findByFormIdAndUserId(FORM_ID, USER_ID))
        .thenReturn(Optional.of(EventApplication.create(form, user(), NOW)));

    assertError(
        () -> service.registerProxy(BOARD_ID, new ProxyApplicationRequest(USER_ID, true)),
        EventApplicationErrorCode.ALREADY_APPLIED);
  }

  @Test
  @DisplayName("취소했던 사람을 대리 등록하면 같은 행을 되살린다")
  void proxyRevivesCanceledApplication() {
    EventApplicationForm form = form(FORM_ID);
    givenForm(form);
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
    EventApplication canceled = EventApplication.create(form, user(), NOW.minusSeconds(600));
    canceled.cancel(NOW.minusSeconds(300));
    when(applicationRepository.findByFormIdAndUserId(FORM_ID, USER_ID))
        .thenReturn(Optional.of(canceled));

    service.registerProxy(BOARD_ID, new ProxyApplicationRequest(USER_ID, true));

    assertThat(canceled.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
    assertThat(canceled.getAttendanceStatus()).isEqualTo(EventAttendanceStatus.ATTENDED);
    verify(applicationRepository, never()).save(any());
  }

  private void givenForm(EventApplicationForm form) {
    when(formRepository.findByEventBoardId(BOARD_ID)).thenReturn(Optional.of(form));
  }

  private static EventApplicationForm form(Long id) {
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
    ReflectionTestUtils.setField(form, "id", id);
    // 부원에게 보이는 경로는 발행된 폼만 찾는다. 픽스처도 발행 상태로 둔다.
    form.publish(Instant.parse("2026-08-01T00:00:00Z"));
    return form;
  }

  private static User user() {
    User user = User.builder().name("홍길동").build();
    ReflectionTestUtils.setField(user, "id", USER_ID);
    return user;
  }

  private void assertError(Runnable action, EventApplicationErrorCode expected) {
    assertThatThrownBy(action::run)
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(expected);
  }
}
