package inha.gdgoc.domain.eventapplication.dto.response;

import inha.gdgoc.domain.eventapplication.entity.EventApplication;
import inha.gdgoc.domain.eventapplication.entity.EventApplicationForm;
import inha.gdgoc.domain.eventapplication.enums.ApplicationStatus;
import inha.gdgoc.domain.eventapplication.enums.EventAttendanceStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 부원이 보는 신청 폼.
 *
 * <p>화면이 버튼 상태를 스스로 계산하지 않도록 {@code canApply} 와 그 사유를 함께 내려준다. 같은 판정을 두 곳에서 하면 어긋난다.
 */
public record EventFormPublicResponse(
    Long eventBoardId,
    String eventTitle,
    LocalDate eventStartDate,
    LocalDate eventEndDate,
    Instant opensAt,
    Instant closesAt,
    Integer capacity,
    long appliedCount,
    Integer remainingSeats,
    boolean canApply,
    String blockedReason,
    List<QuestionResponse> questions,
    MyApplication myApplication) {

  /** 내 신청 상태. 신청한 적이 없으면 null 이다. */
  public record MyApplication(
      ApplicationStatus status,
      EventAttendanceStatus attendanceStatus,
      Instant appliedAt,
      Map<Long, Object> answers) {}

  public static EventFormPublicResponse of(
      EventApplicationForm form,
      long appliedCount,
      boolean canApply,
      String blockedReason,
      EventApplication mine,
      Map<Long, Object> myAnswers) {
    Integer remaining =
        form.getCapacity() == null ? null : Math.max(0, form.getCapacity() - (int) appliedCount);

    MyApplication myApplication =
        mine == null
            ? null
            : new MyApplication(
                mine.getStatus(), mine.getAttendanceStatus(), mine.getAppliedAt(), myAnswers);

    return new EventFormPublicResponse(
        form.getEventBoardId(),
        form.getEventTitle(),
        form.getEventStartDate(),
        form.getEventEndDate(),
        form.getOpensAt(),
        form.getClosesAt(),
        form.getCapacity(),
        appliedCount,
        remaining,
        canApply,
        blockedReason,
        form.activeQuestions().stream().map(QuestionResponse::from).toList(),
        myApplication);
  }
}
