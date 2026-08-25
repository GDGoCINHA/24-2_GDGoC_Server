package inha.gdgoc.domain.admin.recruit.common.dto.response;

import inha.gdgoc.domain.recruit.common.dto.RecruitScheduleNotice;
import inha.gdgoc.domain.recruit.common.dto.RecruitWindow;
import java.time.Instant;

/**
 * 관리자 화면이 보는 모집 기간.
 *
 * @param overridden 화면에서 저장한 값을 쓰고 있으면 true. false 면 서버 설정값이다 — 이걸 알아야
 *     관리자가 '내가 저장한 값이 먹고 있는지'를 판단할 수 있다.
 */
public record RecruitPeriodAdminResponse(
    Instant openAt,
    Instant closeAt,
    boolean overridden,
    RecruitScheduleNotice notice) {

    public static RecruitPeriodAdminResponse of(
        RecruitWindow window, boolean overridden, RecruitScheduleNotice notice) {
        return new RecruitPeriodAdminResponse(
            window.openAt(), window.closeAt(), overridden, notice);
    }
}
