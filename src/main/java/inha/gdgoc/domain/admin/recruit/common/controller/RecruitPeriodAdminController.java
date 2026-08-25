package inha.gdgoc.domain.admin.recruit.common.controller;

import static inha.gdgoc.domain.admin.recruit.common.controller.message.RecruitPeriodAdminMessage.RECRUIT_PERIOD_CLEARED;
import static inha.gdgoc.domain.admin.recruit.common.controller.message.RecruitPeriodAdminMessage.RECRUIT_PERIOD_RETRIEVED;
import static inha.gdgoc.domain.admin.recruit.common.controller.message.RecruitPeriodAdminMessage.RECRUIT_PERIOD_UPDATED;

import inha.gdgoc.domain.admin.recruit.common.dto.request.RecruitPeriodUpdateRequest;
import inha.gdgoc.domain.admin.recruit.common.dto.response.RecruitPeriodAdminResponse;
import inha.gdgoc.domain.recruit.common.dto.RecruitWindow;
import inha.gdgoc.domain.recruit.common.enums.RecruitType;
import inha.gdgoc.domain.recruit.common.service.RecruitPeriodService;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCorePeriodResponse;
import inha.gdgoc.domain.recruit.core.service.RecruitCoreApplicationService;
import inha.gdgoc.domain.recruit.member.dto.response.RecruitMemberPeriodResponse;
import inha.gdgoc.domain.recruit.member.service.RecruitMemberPeriodService;
import inha.gdgoc.domain.user.enums.UserRole;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.security.annotation.Authorize;
import inha.gdgoc.global.security.annotation.Condition;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 모집 기간 관리.
 *
 * <p>여기서 바꾼 값이 지원 창구를 실제로 여닫는다. 화면 문구만 바꾸는 게 아니라 {@code validateOpen()}
 * 이 이 값을 본다 — 그래서 게시판 글쓰기(CORE)가 아니라 LEAD 이상으로 잡고, 바꿀 때마다 로그를 남긴다.
 *
 * <p>저장된 값이 없으면 서버 설정값으로 돌아간다. {@code DELETE} 는 잘못 저장했을 때 되돌리는 길이다 —
 * 이게 없으면 설정값으로 복귀하려면 배포를 해야 한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/recruit/{recruitType}/period")
@RequiredArgsConstructor
@Authorize(@Condition(atLeast = UserRole.LEAD))
public class RecruitPeriodAdminController {

    private final RecruitPeriodService recruitPeriodService;
    private final RecruitCoreApplicationService recruitCoreApplicationService;
    private final RecruitMemberPeriodService recruitMemberPeriodService;

    @GetMapping
    public ResponseEntity<ApiResponse<RecruitPeriodAdminResponse, Void>> getPeriod(
        @PathVariable RecruitType recruitType) {
        return ResponseEntity.ok(
            ApiResponse.ok(RECRUIT_PERIOD_RETRIEVED, describe(recruitType)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<RecruitPeriodAdminResponse, Void>> updatePeriod(
        @AuthenticationPrincipal CustomUserDetails me,
        @PathVariable RecruitType recruitType,
        @Valid @RequestBody RecruitPeriodUpdateRequest request) {

        RecruitWindow saved =
            recruitPeriodService.save(
                recruitType, request.openAt(), request.closeAt(), me.getUserId());

        log.warn(
            "[recruit-period] 기간 변경 - type={}, openAt={}, closeAt={}, by={}",
            recruitType,
            saved.openAt(),
            saved.closeAt(),
            me.getUserId());

        return ResponseEntity.ok(
            ApiResponse.ok(RECRUIT_PERIOD_UPDATED, RecruitPeriodAdminResponse.of(saved, true)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<RecruitPeriodAdminResponse, Void>> clearPeriod(
        @AuthenticationPrincipal CustomUserDetails me, @PathVariable RecruitType recruitType) {

        recruitPeriodService.clear(recruitType);
        log.warn("[recruit-period] 설정값으로 복귀 - type={}, by={}", recruitType, me.getUserId());

        return ResponseEntity.ok(ApiResponse.ok(RECRUIT_PERIOD_CLEARED, describe(recruitType)));
    }

    /**
     * 지금 실제로 쓰이는 기간을 읽는다.
     *
     * <p>저장된 값을 그대로 되돌려주지 않고 기간 서비스에게 묻는다 — 지원 판정이 보는 것과 같은 경로여야
     * 화면에 뜬 값과 실제 동작이 어긋나지 않는다.
     */
    private RecruitPeriodAdminResponse describe(RecruitType recruitType) {
        boolean overridden = recruitPeriodService.find(recruitType).isPresent();

        if (recruitType == RecruitType.CORE) {
            RecruitCorePeriodResponse period = recruitCoreApplicationService.getPeriod();
            return new RecruitPeriodAdminResponse(period.openAt(), period.closeAt(), overridden);
        }

        RecruitMemberPeriodResponse period = recruitMemberPeriodService.getPeriod();
        return new RecruitPeriodAdminResponse(period.openAt(), period.closeAt(), overridden);
    }
}
