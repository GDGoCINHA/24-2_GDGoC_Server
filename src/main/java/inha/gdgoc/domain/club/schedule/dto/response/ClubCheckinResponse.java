package inha.gdgoc.domain.club.schedule.dto.response;

import java.time.Instant;

/**
 * QR 체크인 결과.
 *
 * @param alreadyCheckedIn 이미 체크인돼 있었다. 두 번 찍는 것은 흔하므로 오류가 아니다
 * @param checkedAt 처음 체크인한 시각
 */
public record ClubCheckinResponse(
    Long clubId,
    Long scheduleId,
    String scheduleTitle,
    boolean alreadyCheckedIn,
    Instant checkedAt) {}
