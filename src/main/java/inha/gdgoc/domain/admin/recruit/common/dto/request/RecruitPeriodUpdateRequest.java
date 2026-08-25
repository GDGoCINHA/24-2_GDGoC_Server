package inha.gdgoc.domain.admin.recruit.common.dto.request;

import inha.gdgoc.domain.recruit.common.dto.RecruitScheduleNotice;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * 모집 기간 변경.
 *
 * <p>순서 검증({@code openAt < closeAt})은 {@code RecruitWindow} 가 한다 — 설정에서 오는 값과
 * 화면에서 오는 값이 같은 규칙을 지나게 하려는 것이다.
 */
public record RecruitPeriodUpdateRequest(
    @NotNull Instant openAt,
    @NotNull Instant closeAt,
    Instant documentResultAt,
    Instant interviewOpenAt,
    Instant interviewCloseAt,
    Instant finalResultAt,
    String interviewNote,
    String meetingNote,
    Instant intensiveOpenAt,
    Instant intensiveCloseAt) {

    /**
     * 안내 일정 부분만 떼어낸다.
     *
     * <p>기간과 달리 전부 비어도 된다. 비운 칸은 비운 채로 저장된다 — 화면에서 지웠다는 뜻이라
     * 예전 값을 남겨두면 지운 날짜가 계속 보인다.
     */
    public RecruitScheduleNotice toNotice() {
        return new RecruitScheduleNotice(
            documentResultAt,
            interviewOpenAt,
            interviewCloseAt,
            finalResultAt,
            interviewNote,
            meetingNote,
            intensiveOpenAt,
            intensiveCloseAt);
    }
}
