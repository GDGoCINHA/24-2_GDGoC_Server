package inha.gdgoc.domain.club.schedule.dto.response;

import inha.gdgoc.domain.club.schedule.enums.ClubRsvp;
import java.time.Instant;

/**
 * 일정 한 건과 참석 예정 응답 집계. 집계는 지금 팀원만 센다 — 탈퇴한 사람의 옛 응답은 빠진다.
 *
 * @param onlineLink 팀 멤버·운영진에게만 준다. 단톡방 링크처럼 외부에 돌면 안 된다
 * @param myResponse 내 응답. 응답하지 않았거나 팀원이 아니면 null
 * @param activityId 이 일정에 연결된 활동 기록. 없으면 null — 화면이 「기록 작성」 과 「기록 보기」 를 가른다
 */
public record ClubScheduleResponse(
    Long id,
    String title,
    Instant startsAt,
    String location,
    String onlineLink,
    String description,
    Long activityId,
    long attendCount,
    long absentCount,
    long noResponseCount,
    ClubRsvp myResponse) {}
