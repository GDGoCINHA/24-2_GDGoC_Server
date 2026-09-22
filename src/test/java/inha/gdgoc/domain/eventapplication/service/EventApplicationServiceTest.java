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
import inha.gdgoc.domain.eventapplication.dto.request.AnonymousApplicationRequest;
import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.util.MajorNormalizer;
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
import org.mockito.ArgumentCaptor;
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
            new MajorNormalizer(),
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

  // closesAt 을 안 넣은 폼이 대부분이라 그것만 믿으면 끝난 행사에 신청이 계속 들어온다.
  @Test
  @DisplayName("끝난 행사에는 신청할 수 없다")
  void rejectsAfterEventEnded() {
    givenForm(endedForm());
    givenNoExistingApplication();

    assertError(() -> apply(UserRole.MEMBER), EventApplicationErrorCode.EVENT_ENDED);
    verify(applicationRepository, never()).save(any());
  }

  // 종료일 당일은 아직 행사 중이다. 뒤풀이 자리에서 받는 신청을 막으면 안 된다.
  @Test
  @DisplayName("행사 종료일 당일에는 신청할 수 있다")
  void allowsOnLastEventDay() {
    // NOW 는 2026-09-01 12:00 KST 이고 픽스처의 종료일이 그날이다.
    EventApplicationForm form =
        form(UserRole.MEMBER, null, null, null, true, LocalDate.of(2026, 9, 1));
    givenForm(form);
    givenNoExistingApplication();
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));

    assertThatCode(() -> apply(UserRole.MEMBER)).doesNotThrowAnyException();
  }

  private static EventApplicationForm endedForm() {
    return form(UserRole.MEMBER, null, null, null, true, LocalDate.of(2026, 8, 31));
  }

  @Test
  @DisplayName("발행 전 폼은 부원에게 없는 것으로 보인다")
  void unpublishedFormIsInvisible() {
    // 만드는 중인 폼이 행사 상세에 뜨면 질문을 다 넣기 전에 노출된다.
    // 웹은 404 를 "신청을 받지 않는 행사" 로 읽어 신청 영역을 아예 그리지 않는다.
    givenFormForRead(unpublishedForm());

    assertError(
        () -> service.getForm(BOARD_ID, USER_ID, UserRole.MEMBER),
        EventApplicationErrorCode.FORM_NOT_FOUND);
  }

  @Test
  @DisplayName("발행 전 폼에는 주소를 알아도 신청할 수 없다")
  void cannotApplyToUnpublishedForm() {
    givenForm(unpublishedForm());

    assertError(() -> apply(UserRole.MEMBER), EventApplicationErrorCode.FORM_NOT_FOUND);
    verify(applicationRepository, never()).save(any());
  }

  private static EventApplicationForm unpublishedForm() {
    EventApplicationForm form = form(UserRole.MEMBER, null, null, null, true);
    ReflectionTestUtils.setField(form, "publishedAt", null);
    return form;
  }

  /* ---------------- 로그인 없이 신청 ---------------- */

  @Test
  @DisplayName("로그인 없이 받지 않는 폼에는 비로그인 신청을 막는다")
  void anonymousRejectedWhenFormRequiresLogin() {
    givenForm(form(UserRole.GUEST, null, null, null, true));

    // GUEST 는 "가입했으나 승인 전" 이지 외부인이 아니다. GUEST 폼이라도 비로그인은 못 낸다.
    assertError(() -> applyAnonymously("CSE"), EventApplicationErrorCode.LOGIN_REQUIRED);
    verify(applicationRepository, never()).save(any());
  }

  @Test
  @DisplayName("로그인 없이 받는 폼은 적어 낸 신원으로 신청이 들어간다")
  void anonymousApplicationStoresIdentity() {
    givenForm(anonymousForm());

    applyAnonymously("컴퓨터공학과");

    ArgumentCaptor<EventApplication> saved = ArgumentCaptor.forClass(EventApplication.class);
    verify(applicationRepository).save(saved.capture());
    EventApplication application = saved.getValue();
    assertThat(application.getUser()).isNull();
    assertThat(application.getApplicantName()).isEqualTo("김링크");
    assertThat(application.getApplicantStudentId()).isEqualTo("12241234");
    // 학과명으로 와도 계정과 같은 코드로 저장한다. CSV 가 코드를 학과명으로 되돌린다.
    assertThat(application.getApplicantMajor()).isEqualTo("CSE");
    assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
  }

  @Test
  @DisplayName("같은 학번으로 이미 들어온 신청이 있으면 막는다")
  void anonymousRejectsDuplicateStudentId() {
    EventApplicationForm form = anonymousForm();
    givenForm(form);
    when(applicationRepository.findByFormIdAndStudentId(
            FORM_ID, "12241234", ApplicationStatus.APPLIED))
        .thenReturn(List.of(EventApplication.create(form, user(), NOW)));

    assertError(() -> applyAnonymously("CSE"), EventApplicationErrorCode.ALREADY_APPLIED);
    verify(applicationRepository, never()).save(any());
  }

  @Test
  @DisplayName("목록에 없는 학과는 받지 않는다")
  void anonymousRejectsUnknownMajor() {
    givenForm(anonymousForm());

    assertError(() -> applyAnonymously("없는학과"), EventApplicationErrorCode.MAJOR_INVALID);
  }

  @Test
  @DisplayName("로그인 없이 받는 폼도 마감은 지킨다")
  void anonymousRespectsClosedForm() {
    EventApplicationForm form = anonymousForm();
    form.updateSettings(null, null, null, null, false);
    givenForm(form);

    assertError(() -> applyAnonymously("CSE"), EventApplicationErrorCode.FORM_CLOSED);
  }

  @Test
  @DisplayName("로그인 없이 받는 폼은 역할과 상관없이 로그인한 사람도 신청할 수 있다")
  void loggedInGuestCanApplyToAnonymousForm() {
    givenForm(anonymousForm());
    givenNoExistingApplication();
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));

    assertThatCode(() -> apply(UserRole.GUEST)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("로그인 없이 먼저 낸 학번으로 계정 신청하면 막는다")
  void accountApplicationRejectedWhenAnonymousExists() {
    EventApplicationForm form = anonymousForm();
    givenForm(form);
    givenNoExistingApplication();
    User user = user();
    ReflectionTestUtils.setField(user, "studentId", "12241234");
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(applicationRepository.findByFormIdAndStudentId(
            FORM_ID, "12241234", ApplicationStatus.APPLIED))
        .thenReturn(
            List.of(
                EventApplication.createAnonymous(
                    form, "김링크", "12241234", "CSE", "01012345678", NOW)));

    // 한 사람이 정원을 두 자리 차지하면 안 된다.
    assertError(() -> apply(UserRole.MEMBER), EventApplicationErrorCode.ALREADY_APPLIED);
    verify(applicationRepository, never()).save(any());
  }

  @Test
  @DisplayName("비로그인으로 로그인 필수 폼을 열면 로그인 안내를 사유로 내려준다")
  void anonymousViewOfLoginOnlyForm() {
    givenFormForRead(form(UserRole.MEMBER, null, null, null, true));

    var response = service.getForm(BOARD_ID, null, null);

    assertThat(response.canApply()).isFalse();
    assertThat(response.allowAnonymous()).isFalse();
    assertThat(response.blockedReason())
        .isEqualTo(EventApplicationErrorCode.LOGIN_REQUIRED.getMessage());
    assertThat(response.myApplication()).isNull();
  }

  private void applyAnonymously(String major) {
    service.applyAnonymously(
        BOARD_ID,
        new AnonymousApplicationRequest(
            " 김링크 ", "12241234", major, "010-1234-5678", true, List.of()));
  }

  private static EventApplicationForm anonymousForm() {
    EventApplicationForm form = form(UserRole.MEMBER, null, null, null, true);
    form.changeAllowAnonymous(true);
    return form;
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
    return form(minRole, opensAt, closesAt, capacity, isOpen, LocalDate.of(2026, 9, 2));
  }

  private static EventApplicationForm form(
      UserRole minRole,
      Instant opensAt,
      Instant closesAt,
      Integer capacity,
      boolean isOpen,
      LocalDate eventEndDate) {
    EventApplicationForm form =
        EventApplicationForm.create(
            BOARD_ID,
            "가을 해커톤",
            LocalDate.of(2026, 9, 1),
            eventEndDate,
            opensAt,
            closesAt,
            capacity,
            minRole,
            isOpen);
    ReflectionTestUtils.setField(form, "id", FORM_ID);
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
