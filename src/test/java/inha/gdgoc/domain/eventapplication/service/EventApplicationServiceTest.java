package inha.gdgoc.domain.eventapplication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import inha.gdgoc.domain.eventapplication.dto.request.EventApplicationSubmitRequest;
import inha.gdgoc.domain.eventapplication.entity.EventApplication;
import inha.gdgoc.domain.eventapplication.entity.EventApplicationForm;
import inha.gdgoc.domain.eventapplication.enums.ApplicationStatus;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventApplicationServiceTest {

  private static final Long BOARD_ID = 10L;
  private static final Long FORM_ID = 100L;
  private static final Long USER_ID = 7L;
  private static final Instant NOW = Instant.parse("2026-09-01T03:00:00Z");

  @Mock private EventApplicationFormRepository formRepository;
  @Mock private EventApplicationRepository applicationRepository;
  @Mock private UserRepository userRepository;

  private EventApplicationService service;

  @BeforeEach
  void setUp() {
    service =
        new EventApplicationService(
            formRepository,
            applicationRepository,
            userRepository,
            new AnswerValidator(),
            new AnswerCodec(new ObjectMapper()),
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  @DisplayName("권한이 모자라면 신청을 막는다")
  void rejectsWhenRoleBelowMinimum() {
    givenForm(form(UserRole.MEMBER, null, null, null, true));
    givenNoExistingApplication();

    assertError(() -> apply(UserRole.GUEST), EventApplicationErrorCode.NOT_ELIGIBLE);
    verify(applicationRepository, never()).save(any());
  }

  @Test
  @DisplayName("외부 공개 행사는 GUEST 도 신청할 수 있다")
  void guestCanApplyWhenMinRoleIsGuest() {
    givenForm(form(UserRole.GUEST, null, null, null, true));
    givenNoExistingApplication();
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));

    assertThatCode(() -> apply(UserRole.GUEST)).doesNotThrowAnyException();
    verify(applicationRepository).save(any());
  }

  @Test
  @DisplayName("신청 시작 전이면 막는다")
  void rejectsBeforeOpen() {
    givenForm(form(UserRole.MEMBER, NOW.plusSeconds(3600), null, null, true));
    givenNoExistingApplication();

    assertError(() -> apply(UserRole.MEMBER), EventApplicationErrorCode.NOT_OPEN_YET);
  }

  @Test
  @DisplayName("마감 시각이 되면 막는다")
  void rejectsAtCloseInstant() {
    // 마감 시각 정각은 이미 닫힌 것으로 본다.
    givenForm(form(UserRole.MEMBER, null, NOW, null, true));
    givenNoExistingApplication();

    assertError(() -> apply(UserRole.MEMBER), EventApplicationErrorCode.ALREADY_CLOSED);
  }

  @Test
  @DisplayName("수동으로 닫아두면 기간 안이어도 막는다")
  void rejectsWhenManuallyClosed() {
    givenForm(form(UserRole.MEMBER, null, null, null, false));
    givenNoExistingApplication();

    assertError(() -> apply(UserRole.MEMBER), EventApplicationErrorCode.FORM_CLOSED);
  }

  @Test
  @DisplayName("정원이 찼으면 막는다")
  void rejectsWhenCapacityFull() {
    givenForm(form(UserRole.MEMBER, null, null, 2, true));
    givenNoExistingApplication();
    when(applicationRepository.countByFormIdAndStatus(FORM_ID, ApplicationStatus.APPLIED))
        .thenReturn(2L);

    assertError(() -> apply(UserRole.MEMBER), EventApplicationErrorCode.CAPACITY_FULL);
  }

  @Test
  @DisplayName("정원을 셀 때 폼 행을 잠근다")
  void locksFormRowBeforeCountingCapacity() {
    givenForm(form(UserRole.MEMBER, null, null, 10, true));
    givenNoExistingApplication();
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));

    apply(UserRole.MEMBER);

    // 잠그지 않고 읽으면 동시 신청이 정원을 넘긴다.
    verify(formRepository).findByEventBoardIdForUpdate(BOARD_ID);
  }

  @Test
  @DisplayName("이미 신청했으면 다시 신청할 수 없다")
  void rejectsDuplicateApplication() {
    EventApplicationForm form = form(UserRole.MEMBER, null, null, null, true);
    givenForm(form);
    when(applicationRepository.findByFormIdAndUserId(FORM_ID, USER_ID))
        .thenReturn(Optional.of(EventApplication.create(form, user(), NOW)));

    assertError(() -> apply(UserRole.MEMBER), EventApplicationErrorCode.ALREADY_APPLIED);
  }

  @Test
  @DisplayName("취소했던 신청은 새 행을 만들지 않고 같은 행을 되살린다")
  void reapplyRevivesSameRow() {
    EventApplicationForm form = form(UserRole.MEMBER, null, null, null, true);
    givenForm(form);
    EventApplication canceled = EventApplication.create(form, user(), NOW.minusSeconds(600));
    canceled.cancel(NOW.minusSeconds(300));
    when(applicationRepository.findByFormIdAndUserId(FORM_ID, USER_ID))
        .thenReturn(Optional.of(canceled));

    apply(UserRole.MEMBER);

    assertThat(canceled.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
    assertThat(canceled.getCanceledAt()).isNull();
    // UNIQUE(form_id, user_id) 때문에 새로 저장하면 충돌한다.
    verify(applicationRepository, never()).save(any());
  }

  @Test
  @DisplayName("신청 내역이 없으면 취소할 수 없다")
  void cancelWithoutApplication() {
    givenFormForRead(form(UserRole.MEMBER, null, null, null, true));
    when(applicationRepository.findByFormIdAndUserId(FORM_ID, USER_ID)).thenReturn(Optional.empty());

    assertError(
        () -> service.cancel(BOARD_ID, USER_ID), EventApplicationErrorCode.APPLICATION_NOT_FOUND);
  }

  @Test
  @DisplayName("취소하면 행이 남고 상태만 바뀐다")
  void cancelKeepsRow() {
    EventApplicationForm form = form(UserRole.MEMBER, null, null, null, true);
    givenFormForRead(form);
    EventApplication application = EventApplication.create(form, user(), NOW.minusSeconds(60));
    when(applicationRepository.findByFormIdAndUserId(FORM_ID, USER_ID))
        .thenReturn(Optional.of(application));

    service.cancel(BOARD_ID, USER_ID);

    assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CANCELED);
    assertThat(application.getCanceledAt()).isEqualTo(NOW);
    verify(applicationRepository, never()).delete(any());
  }

  private void apply(UserRole role) {
    service.apply(BOARD_ID, new EventApplicationSubmitRequest(List.of()), USER_ID, role);
  }

  private void givenForm(EventApplicationForm form) {
    when(formRepository.findByEventBoardIdForUpdate(BOARD_ID)).thenReturn(Optional.of(form));
  }

  private void givenFormForRead(EventApplicationForm form) {
    when(formRepository.findByEventBoardId(BOARD_ID)).thenReturn(Optional.of(form));
  }

  private void givenNoExistingApplication() {
    when(applicationRepository.findByFormIdAndUserId(FORM_ID, USER_ID)).thenReturn(Optional.empty());
  }

  private static EventApplicationForm form(
      UserRole minRole, Instant opensAt, Instant closesAt, Integer capacity, boolean isOpen) {
    EventApplicationForm form =
        EventApplicationForm.create(
            BOARD_ID,
            "가을 해커톤",
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 2),
            opensAt,
            closesAt,
            capacity,
            minRole,
            isOpen);
    ReflectionTestUtils.setField(form, "id", FORM_ID);
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
