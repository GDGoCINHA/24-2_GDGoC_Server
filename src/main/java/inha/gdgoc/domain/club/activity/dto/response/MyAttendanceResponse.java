package inha.gdgoc.domain.club.activity.dto.response;

import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import java.time.LocalDate;

/**
 * 한 회차의 내 출석. 일정 탭의 「내 출석」 에 쓴다.
 *
 * @param scheduleTitle 일정에 연결된 기록이면 그 회차 제목, 아니면 null
 * @param status 인증 완료(APPROVED)면 수정 요청을 받지 않는다
 * @param fixRequestPending 내가 낸 수정 요청이 처리 대기 중이다
 */
public record MyAttendanceResponse(
    Long activityId,
    LocalDate activityDate,
    String scheduleTitle,
    ClubActivityStatus status,
    boolean attended,
    boolean fixRequestPending) {}
