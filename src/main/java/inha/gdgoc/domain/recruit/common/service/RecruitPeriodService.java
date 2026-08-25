package inha.gdgoc.domain.recruit.common.service;

import inha.gdgoc.domain.recruit.common.dto.RecruitWindow;
import inha.gdgoc.domain.recruit.common.entity.RecruitPeriodOverride;
import inha.gdgoc.domain.recruit.common.enums.RecruitType;
import inha.gdgoc.domain.recruit.common.repository.RecruitPeriodOverrideRepository;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모집 기간을 읽고 쓴다.
 *
 * <p>캐시하지 않는다. 지원 화면 진입과 제출에서만 불리고, 캐시를 두면 관리자가 기간을 바꿔도 얼마간
 * 예전 값으로 지원이 열리거나 막힌다 — 지원 창구를 여닫는 값이라 그 지연이 위험하다.
 */
@Service
@RequiredArgsConstructor
public class RecruitPeriodService implements RecruitPeriodOverrideReader {

    private final RecruitPeriodOverrideRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<RecruitWindow> find(RecruitType recruitType) {
        return repository
            .findById(recruitType)
            .map(row -> new RecruitWindow(row.getOpenAt(), row.getCloseAt()));
    }

    /** 덮어쓸 기간을 저장한다. 순서 검증은 {@link RecruitWindow} 가 한다. */
    @Transactional
    public RecruitWindow save(
        RecruitType recruitType, Instant openAt, Instant closeAt, Long updatedBy) {
        RecruitWindow window = new RecruitWindow(openAt, closeAt);

        RecruitPeriodOverride row =
            repository
                .findById(recruitType)
                .orElseGet(
                    () ->
                        RecruitPeriodOverride.create(
                            recruitType, window.openAt(), window.closeAt(), updatedBy));
        row.apply(window.openAt(), window.closeAt(), updatedBy);
        repository.save(row);

        return window;
    }

    /**
     * 덮어쓴 기간을 지운다. 지우면 설정값으로 돌아간다.
     *
     * <p>관리자가 잘못 저장했을 때 되돌릴 수단이다. 이게 없으면 설정값으로 복귀하려면 배포를 해야 한다.
     */
    @Transactional
    public void clear(RecruitType recruitType) {
        repository.deleteById(recruitType);
    }
}
