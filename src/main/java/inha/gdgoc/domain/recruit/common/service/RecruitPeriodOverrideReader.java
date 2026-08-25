package inha.gdgoc.domain.recruit.common.service;

import inha.gdgoc.domain.recruit.common.dto.RecruitScheduleNotice;
import inha.gdgoc.domain.recruit.common.dto.RecruitWindow;
import inha.gdgoc.domain.recruit.common.enums.RecruitType;
import java.util.Optional;

/**
 * 설정값을 덮어쓸 기간이 있으면 준다.
 *
 * <p>인터페이스로 둔 이유는 테스트다. 기간 서비스들은 시각과 기간을 직접 받는 생성자를 갖고 있고 그쪽에는
 * {@link #NONE} 을 넘긴다 — 테스트가 DB 를 띄우지 않아도 설정값 경로를 그대로 검증할 수 있다.
 */
@FunctionalInterface
public interface RecruitPeriodOverrideReader {

    /** 덮어쓸 기간이 없으면 비어 있다. 그러면 부르는 쪽이 설정값을 쓴다. */
    Optional<RecruitWindow> find(RecruitType recruitType);

    /**
     * 화면에 보여줄 안내 일정. 저장된 게 없으면 빈 값이고, 그때는 웹이 번들 기본값을 쓴다.
     *
     * <p>기본 구현을 둬서 {@link #NONE} 이 람다로 남는다 — 테스트가 DB 없이 설정값 경로를 검증하는
     * 방식을 그대로 쓸 수 있다. 안내 일정은 지원 판정에 안 쓰이므로 비어 있어도 아무 문제가 없다.
     */
    default RecruitScheduleNotice findNotice(RecruitType recruitType) {
        return RecruitScheduleNotice.empty();
    }

    RecruitPeriodOverrideReader NONE = recruitType -> Optional.empty();
}
