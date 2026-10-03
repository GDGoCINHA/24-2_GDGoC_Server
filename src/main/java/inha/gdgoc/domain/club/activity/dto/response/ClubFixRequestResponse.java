package inha.gdgoc.domain.club.activity.dto.response;

import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import inha.gdgoc.domain.club.activity.enums.ClubFixRequestStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 리더가 처리할 출석 수정 요청 한 건.
 *
 * @param currentAttended 지금 기록된 출석. 요청자가 명단에서 빠졌으면 null(활동일이 바뀐 경우) — 수락할 수 없으니 거절한다
 * @param requestedAttended 요청한 값. 수락하면 출석이 이 값이 된다
 * @param activityStatus 기록이 이미 인증 완료(APPROVED)면 수락할 수 없다
 */
public record ClubFixRequestResponse(
    Long id,
    Long activityId,
    LocalDate activityDate,
    ClubActivityStatus activityStatus,
    Long userId,
    String userName,
    Boolean currentAttended,
    boolean requestedAttended,
    String reason,
    ClubFixRequestStatus status,
    Instant createdAt) {}
