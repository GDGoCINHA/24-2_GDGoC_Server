package inha.gdgoc.domain.homecoming.dto.response;

/**
 * 참석 유형별 인원(취소 제외).
 *
 * @param mainEventAttendees 본 행사에 오는 사람 = main + both (명찰 수)
 * @param afterPartyAttendees 뒤풀이에 오는 사람 = after + both
 */
public record HomecomingSummaryResponse(
    long main,
    long after,
    long both,
    long maybe,
    long no,
    long total,
    long mainEventAttendees,
    long afterPartyAttendees,
    long lateArrivals,
    long canceled) {}
