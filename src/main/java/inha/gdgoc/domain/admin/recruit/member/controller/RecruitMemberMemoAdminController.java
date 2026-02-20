package inha.gdgoc.domain.admin.recruit.member.controller;

import static inha.gdgoc.domain.admin.recruit.member.controller.message.RecruitMemberMemoAdminMessage.MEMBER_MEMO_NOTIFICATION_ENQUEUED;
import static inha.gdgoc.domain.admin.recruit.member.controller.message.RecruitMemberMemoAdminMessage.MEMBER_MEMO_NOTIFICATION_FAILED_RETRIED;
import static inha.gdgoc.domain.admin.recruit.member.controller.message.RecruitMemberMemoAdminMessage.MEMBER_MEMO_NOTIFICATION_TEMPLATE_RETRIEVED;

import inha.gdgoc.domain.admin.recruit.member.dto.request.RecruitMemberMemoOpeningNotificationRequest;
import inha.gdgoc.domain.admin.recruit.member.dto.response.RecruitMemberMemoFailedRetryResponse;
import inha.gdgoc.domain.admin.recruit.member.dto.response.RecruitMemberMemoOpeningNotificationEnqueueResponse;
import inha.gdgoc.domain.admin.recruit.member.dto.response.RecruitMemberMemoNotificationTemplateResponse;
import inha.gdgoc.domain.admin.recruit.member.service.RecruitMemberMemoAdminService;
import inha.gdgoc.global.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/admin/recruit/member/memo/notifications")
public class RecruitMemberMemoAdminController {

    private static final String LEAD_OR_HR_RULE =
            "@accessGuard.check(authentication,"
                    + " T(inha.gdgoc.global.security.AccessGuard$AccessCondition).atLeast("
                    + "T(inha.gdgoc.domain.user.enums.UserRole).LEAD),"
                    + " T(inha.gdgoc.global.security.AccessGuard$AccessCondition).of("
                    + "T(inha.gdgoc.domain.user.enums.UserRole).CORE,"
                    + " T(inha.gdgoc.domain.user.enums.TeamType).HR))";

    private final RecruitMemberMemoAdminService adminService;

    @Operation(summary = "신입생 지원 오픈 알림 메일 기본 문구 조회", security = {@SecurityRequirement(name = "BearerAuth")})
    @PreAuthorize(LEAD_OR_HR_RULE)
    @GetMapping("/template")
    public ResponseEntity<ApiResponse<RecruitMemberMemoNotificationTemplateResponse, Void>> getTemplate() {
        RecruitMemberMemoNotificationTemplateResponse response = adminService.getTemplate();
        return ResponseEntity.ok(ApiResponse.ok(MEMBER_MEMO_NOTIFICATION_TEMPLATE_RETRIEVED, response));
    }

    @Operation(summary = "신입생 지원 오픈 알림 메일 큐 적재", security = {@SecurityRequirement(name = "BearerAuth")})
    @PreAuthorize(LEAD_OR_HR_RULE)
    @PostMapping("/opening")
    public ResponseEntity<ApiResponse<RecruitMemberMemoOpeningNotificationEnqueueResponse, Void>> enqueueOpening(
            @Valid @RequestBody RecruitMemberMemoOpeningNotificationRequest request
    ) {
        RecruitMemberMemoOpeningNotificationEnqueueResponse response = adminService.enqueueOpeningNotifications(request);
        return ResponseEntity.ok(ApiResponse.ok(MEMBER_MEMO_NOTIFICATION_ENQUEUED, response));
    }

    @Operation(summary = "신입생 지원 오픈 알림 메일 실패 건 재시도", security = {@SecurityRequirement(name = "BearerAuth")})
    @PreAuthorize(LEAD_OR_HR_RULE)
    @PostMapping("/retry-failed")
    public ResponseEntity<ApiResponse<RecruitMemberMemoFailedRetryResponse, Void>> retryFailed() {
        RecruitMemberMemoFailedRetryResponse response = adminService.retryFailedNotifications();
        return ResponseEntity.ok(ApiResponse.ok(MEMBER_MEMO_NOTIFICATION_FAILED_RETRIED, response));
    }
}
