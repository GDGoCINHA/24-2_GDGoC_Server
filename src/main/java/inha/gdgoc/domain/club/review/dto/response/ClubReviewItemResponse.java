package inha.gdgoc.domain.club.review.dto.response;

import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 검토 목록 한 건. 필요 인원 충족 여부를 미리 계산해 둔다 — 운영진이 명단을 세지 않아도 되게.
 *
 * @param resubmitted 보완 요청을 받은 뒤 다시 낸 기록인지 (이전 사유가 남아 있다)
 * @param revisionReason 마지막 보완 요청 사유. 재제출이면 무엇을 요청했는지 보여준다
 * @param required 필요 참석 = ceil(명단 × 기수 참석 비율)
 */
public record ClubReviewItemResponse(
    Long activityId,
    Long clubId,
    String clubName,
    LocalDate activityDate,
    String content,
    String progressNote,
    ClubActivityStatus status,
    Instant submittedAt,
    boolean resubmitted,
    String revisionReason,
    List<String> photoUrls,
    List<Attendee> roster,
    int rosterCount,
    int attendedCount,
    int required,
    boolean requiredSatisfied) {

  public record Attendee(Long userId, String name, boolean attended) {}
}
