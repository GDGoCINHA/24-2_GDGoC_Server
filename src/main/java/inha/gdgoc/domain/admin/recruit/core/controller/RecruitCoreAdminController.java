package inha.gdgoc.domain.admin.recruit.core.controller;

import inha.gdgoc.domain.admin.recruit.core.dto.request.RecruitCoreApplicationAcceptRequest;
import inha.gdgoc.domain.admin.recruit.core.dto.request.RecruitCoreApplicationRejectRequest;
import inha.gdgoc.domain.admin.recruit.core.dto.response.RecruitCoreApplicantSummaryResponse;
import inha.gdgoc.domain.admin.recruit.core.dto.response.RecruitCoreApplicationDecisionResponse;
import inha.gdgoc.domain.admin.recruit.core.dto.response.RecruitCoreApplicationPageResponse;
import inha.gdgoc.domain.admin.recruit.core.service.RecruitCoreAdminService;
import inha.gdgoc.domain.recruit.core.entity.RecruitCoreApplication;
import inha.gdgoc.domain.recruit.core.enums.RecruitCoreResultStatus;
import inha.gdgoc.domain.user.enums.TeamType;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/recruit/core/applications")
@RequiredArgsConstructor
public class RecruitCoreAdminController {

    private static final String ORGANIZER_OR_HR_LEAD_RULE =
            "@accessGuard.check(authentication,"
                    + " T(inha.gdgoc.global.security.AccessGuard$AccessCondition).atLeast("
                    + "T(inha.gdgoc.domain.user.enums.UserRole).ORGANIZER),"
                    + " T(inha.gdgoc.global.security.AccessGuard$AccessCondition).of("
                    + "T(inha.gdgoc.domain.user.enums.UserRole).LEAD,"
                    + " T(inha.gdgoc.domain.user.enums.TeamType).HR))";

    private final RecruitCoreAdminService adminService;

    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZER')")
    @GetMapping
    public RecruitCoreApplicationPageResponse list(
        @RequestParam String session,
        @RequestParam(required = false) RecruitCoreResultStatus status,
        @RequestParam(required = false) TeamType team,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RecruitCoreApplication> result = adminService.searchApplications(session, status, team, pageable);
        java.util.List<RecruitCoreApplicantSummaryResponse> content = result
            .map(RecruitCoreApplicantSummaryResponse::from)
            .getContent();
        return RecruitCoreApplicationPageResponse.from(
            content,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.isLast()
        );
    }

    @PreAuthorize(ORGANIZER_OR_HR_LEAD_RULE)
    @PostMapping("/{applicationId}/accept")
    public ResponseEntity<RecruitCoreApplicationDecisionResponse> accept(
        @AuthenticationPrincipal CustomUserDetails reviewer,
        @PathVariable Long applicationId,
        @Valid @RequestBody RecruitCoreApplicationAcceptRequest request
    ) {
        RecruitCoreApplicationDecisionResponse response =
            adminService.accept(applicationId, reviewer.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(ORGANIZER_OR_HR_LEAD_RULE)
    @PostMapping("/{applicationId}/reject")
    public ResponseEntity<RecruitCoreApplicationDecisionResponse> reject(
        @AuthenticationPrincipal CustomUserDetails reviewer,
        @PathVariable Long applicationId,
        @Valid @RequestBody RecruitCoreApplicationRejectRequest request
    ) {
        RecruitCoreApplicationDecisionResponse response =
            adminService.reject(applicationId, reviewer.getUserId(), request);
        return ResponseEntity.ok(response);
    }
}
