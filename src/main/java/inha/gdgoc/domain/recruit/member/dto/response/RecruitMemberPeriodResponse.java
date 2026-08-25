package inha.gdgoc.domain.recruit.member.dto.response;

import inha.gdgoc.domain.recruit.common.dto.RecruitScheduleNotice;
import inha.gdgoc.domain.recruit.member.enums.RecruitMemberPeriodStatus;
import java.time.Instant;

/**
 * 부원 모집 기간.
 *
 * <p>코어의 RecruitCorePeriodResponse 와 같은 모양이되 session 이 없다. 부원 모집은 회차 개념을 쓰지 않는다.
 */
public record RecruitMemberPeriodResponse(
    Instant openAt,
    Instant closeAt,
    RecruitMemberPeriodStatus status,
    RecruitScheduleNotice notice
) {
}
