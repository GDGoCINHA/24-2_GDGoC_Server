package inha.gdgoc.domain.recruit.common.service;

import inha.gdgoc.domain.recruit.common.dto.RecruitScheduleNotice;
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

    /**
     * 화면에 보여줄 안내 일정. 저장된 행이 없으면 빈 값이다 — 그때는 웹이 번들 기본값을 쓴다.
     *
     * <p>{@link #find} 와 나눠 둔다. 저쪽은 지원 판정 경로라 매번 불리는데, 안내 일정까지 함께 실어
     * 보내면 판정에 안 쓰는 값을 계속 읽게 된다.
     */
    @Override
    @Transactional(readOnly = true)
    public RecruitScheduleNotice findNotice(RecruitType recruitType) {
        return repository
            .findById(recruitType)
            .map(RecruitPeriodOverride::toNotice)
            .orElseGet(RecruitScheduleNotice::empty);
    }

    /** 덮어쓸 기간을 저장한다. 순서 검증은 {@link RecruitWindow} 가 한다. */
    @Transactional
    public RecruitWindow save(
        RecruitType recruitType, Instant openAt, Instant closeAt, Long updatedBy) {
        return save(recruitType, openAt, closeAt, null, updatedBy).window();
    }

    /**
     * 기간과 안내 일정을 함께 저장한다.
     *
     * <p>한 번에 저장하는 이유는 이 둘이 관리자 화면에서 한 폼이기 때문이다. 따로 저장하면 기간만
     * 바뀌고 안내는 옛날 날짜로 남는 중간 상태가 생긴다.
     *
     * @param notice null 이면 안내 일정은 건드리지 않는다 (기간만 바꾸는 호출).
     */
    @Transactional
    public SavedPeriod save(
        RecruitType recruitType,
        Instant openAt,
        Instant closeAt,
        RecruitScheduleNotice notice,
        Long updatedBy) {
        RecruitWindow window = new RecruitWindow(openAt, closeAt);

        RecruitPeriodOverride row =
            repository
                .findById(recruitType)
                .orElseGet(
                    () ->
                        RecruitPeriodOverride.create(
                            recruitType, window.openAt(), window.closeAt(), updatedBy));
        row.apply(window.openAt(), window.closeAt(), updatedBy);
        if (notice != null) {
            row.applyNotice(notice);
        }
        repository.save(row);

        return new SavedPeriod(window, row.toNotice());
    }

    /** 저장 직후의 값. 화면이 되돌려 그리는 데 쓴다. */
    public record SavedPeriod(RecruitWindow window, RecruitScheduleNotice notice) {}

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
