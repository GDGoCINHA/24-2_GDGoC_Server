package inha.gdgoc.domain.recruit.controller;

import static inha.gdgoc.domain.recruit.controller.message.RecruitMemberMessage.MEMBER_RETRIEVED_SUCCESS;
import static inha.gdgoc.domain.recruit.controller.message.RecruitMemberMessage.MEMBER_SAVE_SUCCESS;
import static inha.gdgoc.domain.recruit.controller.message.RecruitMemberMessage.PAYMENT_MARKED_COMPLETE_SUCCESS;
import static inha.gdgoc.domain.recruit.controller.message.RecruitMemberMessage.PAYMENT_MARKED_INCOMPLETE_SUCCESS;
import static inha.gdgoc.domain.recruit.controller.message.RecruitMemberMessage.PHONE_NUMBER_DUPLICATION_CHECK_SUCCESS;
import static inha.gdgoc.domain.recruit.controller.message.RecruitMemberMessage.STUDENT_ID_DUPLICATION_CHECK_SUCCESS;

import inha.gdgoc.domain.recruit.dto.request.ApplicationRequest;
import inha.gdgoc.domain.recruit.dto.request.PaymentUpdateRequest;
import inha.gdgoc.domain.recruit.dto.response.CheckPhoneNumberResponse;
import inha.gdgoc.domain.recruit.dto.response.CheckStudentIdResponse;
import inha.gdgoc.domain.recruit.dto.response.SpecifiedMemberResponse;
import inha.gdgoc.domain.recruit.service.RecruitMemberService;
import inha.gdgoc.global.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recruit - Members", description = "리크루팅 지원자 관리 API")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@RestController
public class RecruitMemberController {

    private final RecruitMemberService recruitMemberService;

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<Void, Void>> recruitMemberAdd(
            @RequestBody ApplicationRequest applicationRequest
    ) {
        recruitMemberService.addRecruitMember(applicationRequest);

        return ResponseEntity.ok(ApiResponse.ok(MEMBER_SAVE_SUCCESS));
    }

    @GetMapping("/studentId")
    public ResponseEntity<ApiResponse<CheckStudentIdResponse, Void>> duplicatedStudentIdDetails(
            @RequestParam
            @NotBlank(message = "학번은 필수 입력 값입니다.")
            @Pattern(regexp = "^12[0-9]{6}$", message = "유효하지 않은 학번 값입니다.")
            String studentId
    ) {
        CheckStudentIdResponse response = recruitMemberService.isRegisteredStudentId(studentId);

        return ResponseEntity.ok(ApiResponse.ok(STUDENT_ID_DUPLICATION_CHECK_SUCCESS, response));
    }

    @GetMapping("/check/phoneNumber")
    public ResponseEntity<ApiResponse<CheckPhoneNumberResponse, Void>> duplicatedPhoneNumberDetails(
            @RequestParam
            @NotBlank(message = "전화번호는 필수 입력 값입니다.")
            @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호 형식은 010-XXXX-XXXX 이어야 합니다.")
            String phoneNumber
    ) {
        CheckPhoneNumberResponse response = recruitMemberService
                .isRegisteredPhoneNumber(phoneNumber);

        return ResponseEntity.ok(ApiResponse.ok(PHONE_NUMBER_DUPLICATION_CHECK_SUCCESS, response));
    }

    @Operation(summary = "특정 멤버 가입 신청서 조회", security = {@SecurityRequirement(name = "BearerAuth")})
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/recruit/members/{memberId}")
    public ResponseEntity<ApiResponse<SpecifiedMemberResponse, Void>> getSpecifiedMember(
            @PathVariable Long memberId
    ) {
        SpecifiedMemberResponse response = recruitMemberService.findSpecifiedMember(memberId);

        return ResponseEntity.ok(ApiResponse.ok(MEMBER_RETRIEVED_SUCCESS, response));
    }

    // TODO 전체 응답 조회 및 검색


    @Operation(
            summary = "입금 상태 변경",
            description = "설정하려는 상태(NOT 현재 상태)를 body에 보내주세요. true=입금 완료, false=입금 미완료",
            security = { @SecurityRequirement(name = "BearerAuth") }
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/recruit/members/{memberId}/payment")
    public ResponseEntity<ApiResponse<Void, Void>> updatePayment(
            @PathVariable Long memberId,
            @RequestBody PaymentUpdateRequest paymentUpdateRequest
    ) {
        recruitMemberService.updatePayment(memberId, paymentUpdateRequest.isPayed());

        return ResponseEntity.ok(
                ApiResponse.ok(
                        paymentUpdateRequest.isPayed()
                        ? PAYMENT_MARKED_COMPLETE_SUCCESS
                        : PAYMENT_MARKED_INCOMPLETE_SUCCESS
                )
        );
    }

}
