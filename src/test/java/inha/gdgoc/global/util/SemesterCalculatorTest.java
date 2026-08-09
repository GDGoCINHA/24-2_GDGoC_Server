package inha.gdgoc.global.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import inha.gdgoc.domain.recruit.member.enums.AdmissionSemester;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class SemesterCalculatorTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private SemesterCalculator at(String isoInstant) {
        return new SemesterCalculator(Clock.fixed(Instant.parse(isoInstant), KST));
    }

    @Test
    void 상반기_7월까지는_1학기다() {
        assertThat(at("2026-07-01T03:00:00Z").currentSemester())
            .isEqualTo(AdmissionSemester.Y26_1);
    }

    // 부원 모집 오픈일(2026-08-17 KST). 8월은 2학기로 계산되어 Y26_2 를 찾는다.
    // enum 에 상수가 없으면 여기서 RuntimeException 이 나고 지원서 제출이 통째로 실패한다.
    @Test
    void 부원_모집_오픈일에_학기계산이_성공해야_한다() {
        assertThatCode(() -> at("2026-08-16T15:00:00Z").currentSemester())
            .doesNotThrowAnyException();

        assertThat(at("2026-08-16T15:00:00Z").currentSemester())
            .isEqualTo(AdmissionSemester.Y26_2);
    }

    // 1월은 직전 해의 2학기로 친다. 해가 바뀔 때 상수가 비면 같은 사고가 난다.
    @Test
    void 이듬해_1월은_직전_해_2학기다() {
        assertThat(at("2026-12-31T15:00:00Z").currentSemester())
            .isEqualTo(AdmissionSemester.Y26_2);
    }
}
