package inha.gdgoc.domain.club.activity.dto;

import java.time.LocalDate;

/**
 * 인증 완료된 활동 한 회차. C 의 완주 계산이 받는다.
 *
 * @param rosterCount 활동일 당시 명단 수 — 참석 비율의 분모
 * @param attendedCount 그중 출석한 수
 */
public record ApprovedActivity(
    Long activityId, LocalDate activityDate, long rosterCount, long attendedCount) {}
