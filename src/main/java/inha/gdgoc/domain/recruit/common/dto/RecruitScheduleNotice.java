package inha.gdgoc.domain.recruit.common.dto;

import inha.gdgoc.global.exception.BusinessException;
import inha.gdgoc.global.exception.GlobalErrorCode;
import java.time.Instant;

/**
 * 화면에 보여주는 모집 안내 일정.
 *
 * <p>{@link RecruitWindow} 와 나란히 두되 섞지 않는다. 저쪽은 지원 창구를 여닫는 값이고 이쪽은 안내
 * 문구다 — 이 값이 비어 있어도 지원은 정상적으로 열리고 닫힌다.
 *
 * <p>모든 칸이 비어도 된다. 비면 웹이 번들에 든 기본값을 쓴다. 종류마다 쓰는 칸이 다르다 — CORE 는
 * 서류·면접·최종을, MEMBER 는 집중 모집 기간만 쓴다.
 */
public record RecruitScheduleNotice(
    Instant documentResultAt,
    Instant interviewOpenAt,
    Instant interviewCloseAt,
    Instant finalResultAt,
    String interviewNote,
    String meetingNote,
    Instant intensiveOpenAt,
    Instant intensiveCloseAt) {

    private static final int NOTE_MAX = 300;

    public RecruitScheduleNotice {
        interviewNote = normalize(interviewNote);
        meetingNote = normalize(meetingNote);
        requireOrder(interviewOpenAt, interviewCloseAt, "면접");
        requireOrder(intensiveOpenAt, intensiveCloseAt, "집중 모집");
    }

    /** 아무것도 저장되지 않은 상태. */
    public static RecruitScheduleNotice empty() {
        return new RecruitScheduleNotice(null, null, null, null, null, null, null, null);
    }

    /** 빈 문자열은 null 로 눕힌다 — 화면에서 지웠다는 뜻이라 "빈 문구"로 남기면 안 된다. */
    private static String normalize(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > NOTE_MAX) {
            throw new BusinessException(
                GlobalErrorCode.BAD_REQUEST, "안내 문구는 " + NOTE_MAX + "자를 넘을 수 없습니다.");
        }
        return trimmed;
    }

    /**
     * 한쪽만 채운 상태는 허용한다 — 시작만 정해두고 마감을 나중에 정하는 경우가 있다. 둘 다 있는데
     * 거꾸로인 것만 막는다.
     */
    private static void requireOrder(Instant openAt, Instant closeAt, String label) {
        if (openAt != null && closeAt != null && !openAt.isBefore(closeAt)) {
            throw new BusinessException(
                GlobalErrorCode.BAD_REQUEST, label + " 시작이 마감보다 앞서야 합니다.");
        }
    }
}
