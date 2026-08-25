package inha.gdgoc.domain.recruit.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inha.gdgoc.domain.recruit.common.dto.RecruitWindow;
import inha.gdgoc.domain.recruit.common.enums.RecruitType;
import inha.gdgoc.domain.recruit.member.enums.RecruitMemberPeriodStatus;
import inha.gdgoc.domain.recruit.member.service.RecruitMemberPeriodService;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 관리자가 저장한 기간이 설정값을 이기는지.
 *
 * <p>이 값은 화면 문구가 아니라 지원 창구를 실제로 여닫는다. 덮어쓰기가 판정까지 닿지 않으면 관리자가
 * 기간을 바꿔도 지원은 예전 기간대로 열리고 닫힌다.
 */
class RecruitPeriodOverrideTest {

    private static final Instant CONFIGURED_OPEN =
        OffsetDateTime.parse("2026-08-17T00:00:00+09:00").toInstant();
    private static final Instant CONFIGURED_CLOSE =
        OffsetDateTime.parse("2026-09-09T23:59:59+09:00").toInstant();

    private static final Instant OVERRIDE_OPEN =
        OffsetDateTime.parse("2026-10-01T00:00:00+09:00").toInstant();
    private static final Instant OVERRIDE_CLOSE =
        OffsetDateTime.parse("2026-10-31T23:59:59+09:00").toInstant();

    private RecruitMemberPeriodService serviceAt(
        String isoInstant, RecruitPeriodOverrideReader reader) {
        Clock clock = Clock.fixed(Instant.parse(isoInstant), ZoneId.of("Asia/Seoul"));
        return new RecruitMemberPeriodService(CONFIGURED_OPEN, CONFIGURED_CLOSE, clock, reader);
    }

    private RecruitPeriodOverrideReader overrideOf(Instant openAt, Instant closeAt) {
        return recruitType ->
            recruitType == RecruitType.MEMBER
                ? Optional.of(new RecruitWindow(openAt, closeAt))
                : Optional.empty();
    }

    @Test
    @DisplayName("저장된 기간이 없으면 설정값으로 판정한다")
    void fallsBackToConfiguredWindow() {
        // KST 8/20 — 설정 기간 안쪽
        RecruitMemberPeriodService service =
            serviceAt("2026-08-20T03:00:00Z", RecruitPeriodOverrideReader.NONE);

        assertThat(service.getPeriodStatus()).isEqualTo(RecruitMemberPeriodStatus.OPEN);
        assertThat(service.getPeriod().openAt()).isEqualTo(CONFIGURED_OPEN);
    }

    @Test
    @DisplayName("저장된 기간이 있으면 설정값 대신 그것으로 판정한다")
    void overrideWinsOverConfiguredWindow() {
        // KST 8/20 — 설정 기간 안쪽이지만 저장된 기간(10월)은 아직 열리지 않았다.
        RecruitMemberPeriodService service =
            serviceAt("2026-08-20T03:00:00Z", overrideOf(OVERRIDE_OPEN, OVERRIDE_CLOSE));

        assertThat(service.getPeriodStatus()).isEqualTo(RecruitMemberPeriodStatus.BEFORE_OPEN);
        assertThatThrownBy(service::validateOpen).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("설정 기간이 지났어도 저장된 기간 안이면 지원이 열린다")
    void overrideCanReopenAfterConfiguredClose() {
        // KST 10/15 — 설정 기간(9/9)은 지났지만 저장된 기간 안쪽이다.
        RecruitMemberPeriodService service =
            serviceAt("2026-10-15T03:00:00Z", overrideOf(OVERRIDE_OPEN, OVERRIDE_CLOSE));

        assertThat(service.getPeriodStatus()).isEqualTo(RecruitMemberPeriodStatus.OPEN);
        assertThatCode(service::validateOpen).doesNotThrowAnyException();
        assertThat(service.getPeriod().closeAt()).isEqualTo(OVERRIDE_CLOSE);
    }

    @Test
    @DisplayName("열림이 닫힘보다 뒤인 기간은 만들 수 없다")
    void rejectsInvertedWindow() {
        assertThatThrownBy(() -> new RecruitWindow(OVERRIDE_CLOSE, OVERRIDE_OPEN))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
