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
 *
 * <p>publishedAt 이 null 이면 아직 만드는 중이라 부원에게 보이지 않는다. 운영진 화면만 이 상태를 볼 수 있다.
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
    Instant publishedAt,
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
        form.getPublishedAt(),
        appliedCount,
        form.activeQuestions().stream().map(QuestionResponse::from).toList());
  }
}
