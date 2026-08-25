package inha.gdgoc.domain.recruit.common.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inha.gdgoc.global.exception.BusinessException;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 안내 일정이 화면에 그대로 그려지므로, 저장되는 모양을 여기서 정리한다.
 *
 * <p>{@link RecruitWindow} 와 달리 전부 비어도 되는 값이다. 그래서 "비어 있어도 통과한다" 를 함께
 * 못 박아 둔다 — 여기서 예외가 나면 아직 일정을 안 정한 상태에서 기간 저장 자체가 막힌다.
 */
class RecruitScheduleNoticeTest {

    private static final Instant NINTH = OffsetDateTime.parse("2026-09-09T00:00:00+09:00").toInstant();
    private static final Instant THIRTEENTH =
        OffsetDateTime.parse("2026-09-13T23:59:59+09:00").toInstant();

    private RecruitScheduleNotice interview(Instant openAt, Instant closeAt) {
        return new RecruitScheduleNotice(null, openAt, closeAt, null, null, null, null, null);
    }

    @Test
    @DisplayName("전부 비어 있어도 만들어진다 - 일정을 아직 안 정한 상태다")
    void allowsEmpty() {
        assertThatCode(RecruitScheduleNotice::empty).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("한쪽만 채운 기간은 통과한다 - 시작만 정하고 마감을 나중에 정하는 경우가 있다")
    void allowsHalfFilledPeriod() {
        assertThatCode(() -> interview(NINTH, null)).doesNotThrowAnyException();
        assertThatCode(() -> interview(null, THIRTEENTH)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("둘 다 있는데 거꾸로면 막는다")
    void rejectsReversedPeriod() {
        assertThatThrownBy(() -> interview(THIRTEENTH, NINTH))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("면접");
    }

    @Test
    @DisplayName("공백만 남은 문구는 null 로 눕힌다 - 화면에서 지웠다는 뜻이다")
    void blankNoteBecomesNull() {
        RecruitScheduleNotice notice =
            new RecruitScheduleNotice(null, null, null, null, "   ", "  안내  ", null, null);

        assertThat(notice.interviewNote()).isNull();
        assertThat(notice.meetingNote()).isEqualTo("안내");
    }

    @Test
    @DisplayName("문구가 컬럼 길이를 넘으면 막는다 - 저장 시점에 깨지면 원인을 못 찾는다")
    void rejectsTooLongNote() {
        String tooLong = "가".repeat(301);

        assertThatThrownBy(
                () -> new RecruitScheduleNotice(null, null, null, null, tooLong, null, null, null))
            .isInstanceOf(BusinessException.class);
    }
}
