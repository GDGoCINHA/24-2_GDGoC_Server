package inha.gdgoc.domain.admin.recruit.common.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * 모집 기간 변경.
 *
 * <p>순서 검증({@code openAt < closeAt})은 {@code RecruitWindow} 가 한다 — 설정에서 오는 값과
 * 화면에서 오는 값이 같은 규칙을 지나게 하려는 것이다.
 */
public record RecruitPeriodUpdateRequest(@NotNull Instant openAt, @NotNull Instant closeAt) {
}
