package inha.gdgoc.domain.recruit.core.dto.response;

import inha.gdgoc.domain.recruit.core.enums.RecruitCorePeriodStatus;
import java.time.Instant;

public record RecruitCorePeriodResponse(
    String session,
    Instant openAt,
    Instant closeAt,
    RecruitCorePeriodStatus status
) {
}
