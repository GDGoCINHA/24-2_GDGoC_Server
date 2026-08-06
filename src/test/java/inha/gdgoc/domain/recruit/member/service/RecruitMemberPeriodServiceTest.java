package inha.gdgoc.domain.recruit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inha.gdgoc.domain.recruit.member.enums.RecruitMemberPeriodStatus;
import inha.gdgoc.domain.recruit.member.exception.RecruitMemberErrorCode;
import inha.gdgoc.global.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 부원 모집 기간 판정. 시각을 주입해 마감 전후를 실제로 검증한다. */
class RecruitMemberPeriodServiceTest {

    private static final Instant OPEN_AT =
        OffsetDateTime.parse("2026-08-17T00:00:00+09:00").toInstant();
    private static final Instant CLOSE_AT =
        OffsetDateTime.parse("2026-09-09T23:59:59+09:00").toInstant();

    private RecruitMemberPeriodService serviceAt(String isoInstant) {
        Clock clock = Clock.fixed(Instant.parse(isoInstant), ZoneId.of("Asia/Seoul"));
        return new RecruitMemberPeriodService(OPEN_AT, CLOSE_AT, clock);
    }

    @Test
    @DisplayName("오픈 전에는 BEFORE_OPEN 이고 지원이 막힌다")
    void beforeOpenIsBlocked() {
        RecruitMemberPeriodService service = serviceAt("2026-08-16T14:59:59Z"); // KST 8/16 23:59:59

        assertThat(service.getPeriodStatus()).isEqualTo(RecruitMemberPeriodStatus.BEFORE_OPEN);
        assertThatThrownBy(service::validateOpen)
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(RecruitMemberErrorCode.RECRUIT_MEMBER_NOT_OPEN);
    }

    @Test
    @DisplayName("오픈 시각이 되면 열린다")
    void opensExactlyAtOpenAt() {
        RecruitMemberPeriodService service = serviceAt("2026-08-16T15:00:00Z"); // KST 8/17 00:00:00

        assertThat(service.getPeriodStatus()).isEqualTo(RecruitMemberPeriodStatus.OPEN);
        assertThatCode(service::validateOpen).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("마감 시각 그 순간까지는 열려 있다")
    void remainsOpenExactlyAtCloseAt() {
        RecruitMemberPeriodService service = serviceAt("2026-09-09T14:59:59Z"); // KST 9/9 23:59:59

        assertThat(service.getPeriodStatus()).isEqualTo(RecruitMemberPeriodStatus.OPEN);
        assertThatCode(service::validateOpen).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("마감 후에는 CLOSED 이고 지원이 막힌다")
    void afterCloseIsBlocked() {
        RecruitMemberPeriodService service = serviceAt("2026-09-09T15:00:00Z"); // KST 9/10 00:00:00

        assertThat(service.getPeriodStatus()).isEqualTo(RecruitMemberPeriodStatus.CLOSED);
        assertThatThrownBy(service::validateOpen)
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(RecruitMemberErrorCode.RECRUIT_MEMBER_CLOSED);
    }

    @Test
    @DisplayName("getPeriod 는 설정된 기간을 그대로 준다")
    void periodExposesConfiguredWindow() {
        RecruitMemberPeriodService service = serviceAt("2026-08-20T00:00:00Z");

        assertThat(service.getPeriod().openAt()).isEqualTo(OPEN_AT);
        assertThat(service.getPeriod().closeAt()).isEqualTo(CLOSE_AT);
        assertThat(service.getPeriod().status()).isEqualTo(RecruitMemberPeriodStatus.OPEN);
    }

    @Test
    @DisplayName("open 이 close 보다 뒤면 기동 시점에 막는다")
    void invalidWindowIsRejected() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), ZoneId.of("Asia/Seoul"));

        assertThatThrownBy(() -> new RecruitMemberPeriodService(CLOSE_AT, OPEN_AT, clock))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
