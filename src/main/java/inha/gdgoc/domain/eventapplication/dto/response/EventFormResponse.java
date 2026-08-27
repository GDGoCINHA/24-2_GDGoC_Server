package inha.gdgoc.domain.eventapplication.dto.response;

import inha.gdgoc.domain.eventapplication.entity.EventApplicationForm;
import inha.gdgoc.domain.user.enums.UserRole;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 운영진이 보는 신청 폼.
 *
 * <p>appliedCount 는 폼을 어디까지 고칠 수 있는지 가르는 값이라 함께 내려준다. 0 이면 무엇이든 바꿀 수 있고, 1 이상이면 유형·선택지·필수 강화가 막힌다.
 */
public record EventFormResponse(
    Long id,
    Long eventBoardId,
    String eventTitle,
    LocalDate eventStartDate,
    LocalDate eventEndDate,
    Instant opensAt,
    Instant closesAt,
    Integer capacity,
    UserRole minRole,
    boolean isOpen,
    long appliedCount,
    List<QuestionResponse> questions) {

  public static EventFormResponse of(EventApplicationForm form, long appliedCount) {
    return new EventFormResponse(
        form.getId(),
        form.getEventBoardId(),
        form.getEventTitle(),
        form.getEventStartDate(),
        form.getEventEndDate(),
        form.getOpensAt(),
        form.getClosesAt(),
        form.getCapacity(),
        form.getMinRole(),
        form.isOpen(),
        appliedCount,
        form.activeQuestions().stream().map(QuestionResponse::from).toList());
  }
}
