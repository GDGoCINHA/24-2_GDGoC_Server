package inha.gdgoc.domain.recruit.core.dto.response;

import inha.gdgoc.domain.recruit.common.dto.RecruitScheduleNotice;
import inha.gdgoc.domain.recruit.core.enums.RecruitCorePeriodStatus;
import java.time.Instant;

/**
 * @param notice 서류 발표·면접·최종 발표처럼 화면에만 쓰는 안내 일정. 저장된 게 없으면 칸이 전부
 *     비어 있고, 그때는 웹이 번들 기본값을 그린다. 지원 가능 여부와는 무관하다.
 */
public record RecruitCorePeriodResponse(
    String session,
    Instant openAt,
    Instant closeAt,
    RecruitCorePeriodStatus status,
    RecruitScheduleNotice notice
) {
}
