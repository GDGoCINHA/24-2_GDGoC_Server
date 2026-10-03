package inha.gdgoc.domain.club.schedule.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * 일정 등록·수정. 장소와 온라인 링크는 둘 다 선택이다(기획 "장소 또는 온라인 링크").
 *
 * @param startsAt ISO-8601 시각 (예: {@code 2026-10-07T10:00:00Z})
 */
public record ClubScheduleRequest(
    @NotBlank @Size(max = 200) String title,
    @NotNull Instant startsAt,
    @Size(max = 200) String location,
    @Size(max = 500) String onlineLink,
    String description) {}
