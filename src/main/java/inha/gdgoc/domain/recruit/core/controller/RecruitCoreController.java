package inha.gdgoc.domain.recruit.core.controller;

import inha.gdgoc.domain.recruit.core.dto.request.RecruitCoreApplicationCreateRequest;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCoreApplicantDetailResponse;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCoreApplicationCreateResponse;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCoreEligibilityResponse;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCoreMyApplicationResponse;
import inha.gdgoc.domain.recruit.core.dto.response.RecruitCorePrefillResponse;
import inha.gdgoc.domain.recruit.core.service.RecruitCoreApplicationService;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recruit Core - Guest", description = "운영진 리크루팅 지원 API")
@RestController
@RequestMapping("/api/v1/recruit/core")
@RequiredArgsConstructor
public class RecruitCoreController {

    private final RecruitCoreApplicationService service;

    @Operation(summary = "지원 가능 여부 확인", security = {@SecurityRequirement(name = "BearerAuth")})
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/eligibility")
    public ResponseEntity<RecruitCoreEligibilityResponse> eligibility(
        @AuthenticationPrincipal CustomUserDetails me
    ) {
        RecruitCoreEligibilityResponse response = service.checkEligibility(me.getUserId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "지원서 기본 정보 자동 채움", security = {@SecurityRequirement(name = "BearerAuth")})
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/prefill")
    public ResponseEntity<RecruitCorePrefillResponse> prefill(
        @AuthenticationPrincipal CustomUserDetails me
    ) {
        RecruitCorePrefillResponse response = service.prefill(me.getUserId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "운영진 지원서 제출", security = {@SecurityRequirement(name = "BearerAuth")})
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/applications")
    public ResponseEntity<RecruitCoreApplicationCreateResponse> submit(
        @AuthenticationPrincipal CustomUserDetails me,
        @Valid @RequestBody RecruitCoreApplicationCreateRequest request
    ) {
        RecruitCoreApplicationCreateResponse response = service.submit(me.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "나의 지원서 조회", security = {@SecurityRequirement(name = "BearerAuth")})
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/applications/me")
    public ResponseEntity<RecruitCoreMyApplicationResponse> myApplication(
        @AuthenticationPrincipal CustomUserDetails me
    ) {
        RecruitCoreMyApplicationResponse response = service.getMyApplication(me.getUserId());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "지원서 상세 조회 (본인/운영진)",
        security = {@SecurityRequirement(name = "BearerAuth")}
    )
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<RecruitCoreApplicantDetailResponse> getApplication(
        @AuthenticationPrincipal CustomUserDetails me,
        @PathVariable Long applicationId
    ) {
        RecruitCoreApplicantDetailResponse response =
            service.getApplicantDetailForViewer(applicationId, me.getUserId(), me.getRole());
        return ResponseEntity.ok(response);
    }
}
