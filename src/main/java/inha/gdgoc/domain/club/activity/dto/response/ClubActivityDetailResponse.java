package inha.gdgoc.domain.club.activity.dto.response;

import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 활동 기록 상세.
 *
 * <p><b>{@code attendance}·{@code revisionReason} 은 팀 멤버와 운영진에게만 채운다.</b> 다른 부원에게는 null 이고 인원 수만
 * 보인다(기획 2.3).
 *
 * @param requiredCount 활동일 명단 수 × 기수 참석 비율, 올림
 * @param editable 지금 사용자가 리더이고 아직 인증 완료되지 않았는가
 */
public record ClubActivityDetailResponse(
    Long id,
    Long clubId,
    Long scheduleId,
    LocalDate activityDate,
    String content,
    String progressNote,
    ClubActivityStatus status,
    List<String> photoUrls,
    long rosterCount,
    long attendedCount,
    long requiredCount,
    List<Attendance> attendance,
    String revisionReason,
    Instant submittedAt,
    boolean editable) {

  public record Attendance(Long userId, String name, boolean attended) {}
}
