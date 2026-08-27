package inha.gdgoc.domain.eventapplication.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

/**
 * 행사 참석 상태.
 *
 * <p>코어 정기모임의 {@code AttendanceStatus}(출석·지각·사전승인·결석)와 일부러 분리했다. 정기모임은 코어 전원이 대상이고 날짜당 한 번이지만, 행사는
 * 신청자만 대상이고 하루에 여러 개가 열리며 사실상 왔다·안 왔다뿐이다.
 */
@Getter
public enum EventAttendanceStatus {
  PENDING("미확인"),
  ATTENDED("참석"),
  NO_SHOW("불참");

  private final String label;

  EventAttendanceStatus(String label) {
    this.label = label;
  }

  @JsonCreator
  public static EventAttendanceStatus from(String raw) {
    if (raw == null) {
      return null;
    }
    String normalized = raw.trim().replace('-', '_').replace(' ', '_').toUpperCase();
    if (normalized.isBlank()) {
      return null;
    }
    return switch (normalized) {
      case "PENDING" -> PENDING;
      case "ATTENDED" -> ATTENDED;
      case "NO_SHOW", "NOSHOW", "ABSENT" -> NO_SHOW;
      default -> throw new IllegalArgumentException("Unknown attendance status: " + raw);
    };
  }
}
