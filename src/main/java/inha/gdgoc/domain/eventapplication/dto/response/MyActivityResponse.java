package inha.gdgoc.domain.eventapplication.dto.response;

import inha.gdgoc.domain.eventapplication.entity.EventApplication;
import inha.gdgoc.domain.eventapplication.enums.EventAttendanceStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 마이페이지의 활동 한 줄.
 *
 * <p>행사명과 기간을 폼의 복사본에서 읽는다. 게시글이 휴지통에 들어가도 이력이 빈칸이 되지 않는 이유다. {@code eventBoardId} 는 링크를 걸 때만 쓰고, 글이
 * 사라졌으면 화면이 링크를 빼면 된다.
 */
public record MyActivityResponse(
    Long applicationId,
    Long eventBoardId,
    String eventTitle,
    LocalDate eventStartDate,
    LocalDate eventEndDate,
    Instant appliedAt,
    EventAttendanceStatus attendanceStatus) {

  public static MyActivityResponse from(EventApplication application) {
    var form = application.getForm();
    return new MyActivityResponse(
        application.getId(),
        form.getEventBoardId(),
        form.getEventTitle(),
        form.getEventStartDate(),
        form.getEventEndDate(),
        application.getAppliedAt(),
        application.getAttendanceStatus());
  }
}
