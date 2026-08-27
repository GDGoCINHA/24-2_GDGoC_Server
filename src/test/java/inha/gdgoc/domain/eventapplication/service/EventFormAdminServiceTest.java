package inha.gdgoc.domain.eventapplication.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inha.gdgoc.domain.board.event.entity.EventBoard;
import inha.gdgoc.domain.board.event.repository.EventBoardRepository;
import inha.gdgoc.domain.eventapplication.dto.request.EventFormSaveRequest;
import inha.gdgoc.domain.eventapplication.entity.EventApplicationForm;
import inha.gdgoc.domain.eventapplication.exception.EventApplicationErrorCode;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationFormRepository;
import inha.gdgoc.domain.eventapplication.repository.EventApplicationRepository;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * 신청 폼을 만들 수 있는 조건.
 *
 * <p>끝난 행사에 폼을 만들어 봐야 아무도 신청할 수 없다. 운영진이 지난 글에 잘못 붙이는 것을 여기서 막는다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventFormAdminServiceTest {

  private static final Long BOARD_ID = 10L;
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  /** 2026-09-01 12:00 KST */
  private static final Instant NOW = Instant.parse("2026-09-01T03:00:00Z");

  @Mock private EventApplicationFormRepository formRepository;
  @Mock private EventApplicationRepository applicationRepository;
  @Mock private EventBoardRepository eventBoardRepository;

  private EventFormAdminService service;

  @BeforeEach
  void setUp() {
    service =
        new EventFormAdminService(
            formRepository,
            applicationRepository,
            eventBoardRepository,
            new EventFormValidator(),
            Clock.fixed(NOW, KST));
    when(formRepository.existsByEventBoardId(BOARD_ID)).thenReturn(false);
    when(formRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @DisplayName("끝난 행사에는 신청 폼을 만들 수 없다")
  void rejectsEndedEvent() {
    givenBoard(LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 25));

    assertThatThrownBy(() -> service.createForm(BOARD_ID, request()))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(EventApplicationErrorCode.EVENT_ENDED);

    verify(formRepository, never()).save(any());
  }

  // 오늘 끝나는 행사는 아직 끝난 것이 아니다.
  @Test
  @DisplayName("종료일이 오늘이면 만들 수 있다")
  void allowsEventEndingToday() {
    givenBoard(LocalDate.of(2026, 8, 30), LocalDate.of(2026, 9, 1));

    assertThatCode(() -> service.createForm(BOARD_ID, request())).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("앞으로 열릴 행사는 만들 수 있다")
  void allowsUpcomingEvent() {
    givenBoard(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 11));

    assertThatCode(() -> service.createForm(BOARD_ID, request())).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("휴지통에 있는 글에는 만들 수 없다")
  void rejectsDeletedBoard() {
    when(eventBoardRepository.findById(BOARD_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.createForm(BOARD_ID, request()))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(EventApplicationErrorCode.EVENT_BOARD_NOT_FOUND);
  }

  @Test
  @DisplayName("신청자가 있으면 폼을 지울 수 없다")
  void rejectsDeleteWhenApplied() {
    EventApplicationForm form = EventApplicationForm.create(
        BOARD_ID, "가을 해커톤", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 11),
        null, null, null, UserRole.MEMBER, true);
    when(formRepository.findByEventBoardId(BOARD_ID)).thenReturn(Optional.of(form));
    when(applicationRepository.countByFormIdAndStatus(any(), any())).thenReturn(1L);

    assertThatThrownBy(() -> service.deleteForm(BOARD_ID))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(EventApplicationErrorCode.FORM_HAS_APPLICATIONS);

    verify(formRepository, never()).delete(any());
  }

  private void givenBoard(LocalDate start, LocalDate end) {
    EventBoard board =
        EventBoard.create("가을 해커톤", start, end, null, null, "내용", true, 1L, "운영진");
    when(eventBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
  }

  private static EventFormSaveRequest request() {
    return new EventFormSaveRequest(null, null, null, false, UserRole.MEMBER, true);
  }
}
