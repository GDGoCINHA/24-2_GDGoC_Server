package inha.gdgoc.domain.homecoming.dto.response;

import inha.gdgoc.domain.homecoming.entity.HomecomingApplication;
import inha.gdgoc.domain.homecoming.enums.AttendType;
import inha.gdgoc.domain.homecoming.enums.ConsentState;
import inha.gdgoc.domain.homecoming.enums.HomecomingMemberRole;
import inha.gdgoc.domain.homecoming.enums.HomecomingStatus;
import inha.gdgoc.domain.homecoming.enums.HomecomingTeam;
import java.time.Instant;

/** 관리 화면의 신청 한 줄. 선택 동의는 현재 상태(동의·거부·철회·만료)로 보여준다. */
public record HomecomingApplicationResponse(
    Long id,
    AttendType attendType,
    String attendLabel,
    String notionStatus,
    String name,
    String phone,
    Integer yearStart,
    Integer yearEnd,
    HomecomingMemberRole memberRole,
    HomecomingTeam team,
    String activitySummary,
    boolean late,
    String arrivalTime,
    String message,
    HomecomingStatus status,
    Instant submittedAt,
    Instant canceledAt,
    String cancelReason,
    ConsentState eventInvite,
    ConsentState talentPool) {

  public static HomecomingApplicationResponse of(
      HomecomingApplication a, ConsentState eventInvite, ConsentState talentPool) {
    return new HomecomingApplicationResponse(
        a.getId(),
        a.getAttendType(),
        a.getAttendType().getLabel(),
        a.getAttendType().getNotionStatus(),
        a.getName(),
        a.getPhone(),
        a.getYearStart(),
        a.getYearEnd(),
        a.getMemberRole(),
        a.getTeam(),
        a.activitySummary(),
        a.isLate(),
        a.getArrivalTime(),
        a.getMessage(),
        a.getStatus(),
        a.getSubmittedAt(),
        a.getCanceledAt(),
        a.getCancelReason(),
        eventInvite,
        talentPool);
  }
}
