package inha.gdgoc.domain.eventapplication.dto.response;

import inha.gdgoc.domain.eventapplication.entity.EventApplication;
import inha.gdgoc.domain.eventapplication.enums.ApplicationStatus;
import inha.gdgoc.domain.eventapplication.enums.EventAttendanceStatus;
import java.time.Instant;
import java.util.Map;

/**
 * 운영진이 보는 신청자 한 명.
 *
 * <p>{@code checkedInAt} 이 있으면 QR 로 체크인한 것이고, 비어 있는데 참석으로 되어 있으면 운영진이 수기로 찍은 것이다.
 */
public record ApplicantResponse(
    Long applicationId,
    Long userId,
    String name,
    String studentId,
    String major,
    String email,
    String phoneNumber,
    ApplicationStatus status,
    EventAttendanceStatus attendanceStatus,
    Instant appliedAt,
    Instant canceledAt,
    Instant checkedInAt,
    Map<Long, Object> answers) {

  public static ApplicantResponse of(EventApplication application, Map<Long, Object> answers) {
    var user = application.getUser();
    return new ApplicantResponse(
        application.getId(),
        user.getId(),
        user.getName(),
        user.getStudentId(),
        user.getMajor(),
        user.getEmail(),
        user.getPhoneNumber(),
        application.getStatus(),
        application.getAttendanceStatus(),
        application.getAppliedAt(),
        application.getCanceledAt(),
        application.getCheckedInAt(),
        answers);
  }
}
