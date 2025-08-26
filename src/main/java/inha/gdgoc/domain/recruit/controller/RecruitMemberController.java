package inha.gdgoc.domain.recruit.controller;

import static inha.gdgoc.domain.recruit.controller.message.RecruitMemberMessage.MEMBER_RETRIEVED_SUCCESS;
import static inha.gdgoc.domain.recruit.controller.message.RecruitMemberMessage.MEMBER_SAVE_SUCCESS;
import static inha.gdgoc.domain.recruit.controller.message.RecruitMemberMessage.PHONE_NUMBER_DUPLICATION_CHECK_SUCCESS;
import static inha.gdgoc.domain.recruit.controller.message.RecruitMemberMessage.STUDENT_ID_DUPLICATION_CHECK_SUCCESS;

import inha.gdgoc.domain.recruit.dto.request.ApplicationRequest;
import inha.gdgoc.domain.recruit.dto.response.CheckPhoneNumberResponse;
import inha.gdgoc.domain.recruit.dto.response.CheckStudentIdResponse;
import inha.gdgoc.domain.recruit.dto.response.SpecifiedMemberResponse;
import inha.gdgoc.domain.recruit.service.RecruitMemberService;
import inha.gdgoc.global.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1")
@RequiredArgsConstructor
@RestController
public class RecruitMemberController {

    private final RecruitMemberService recruitMemberService;

    /**
     * 지원서 신청을 등록합니다.
     *
     * 요청 본문의 ApplicationRequest를 받아 가입 신청을 저장하도록 서비스에 위임하고,
     * 저장 성공 시 MEMBER_SAVE_SUCCESS 메시지를 담은 200 OK 응답을 반환합니다.
     *
     * @param applicationRequest 클라이언트로부터 전달된 가입 신청 DTO
     * @return 저장 성공 메시지를 포함한 200 OK 응답 (ApiResponse<Void, Void> 래퍼)
     */
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<Void, Void>> recruitMemberAdd(
            @RequestBody ApplicationRequest applicationRequest
    ) {
        recruitMemberService.addRecruitMember(applicationRequest);

        return ResponseEntity.ok(ApiResponse.ok(MEMBER_SAVE_SUCCESS));
    }

    /**
     * 학번 중복 여부를 조회합니다.
     *
     * 주어진 학번으로 등록 여부를 확인한 후 검사 결과를 담은 {@link CheckStudentIdResponse}를 ApiResponse로 래핑하여 반환합니다.
     *
     * @param studentId 확인할 학번(형식: "12XXXXXX", 총 8자리)
     * @return 중복 검사 성공 메시지와 함께 검사 결과를 포함한 {@code ResponseEntity<ApiResponse<CheckStudentIdResponse, Void>>}
     */
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

    /**
     * 전화번호 중복 여부를 조회한다.
     *
     * 요청된 전화번호(형식: 010-XXXX-XXXX, 예: 010-1234-5678)의 가입 여부를 검사하여
     * 중복 체크 결과를 담은 ApiResponse를 포함한 HTTP 200 응답을 반환한다.
     *
     * @param phoneNumber 검사할 전화번호(필수, 패턴: ^010-\d{4}-\d{4}$)
     * @return 중복 검사 결과를 포함한 ApiResponse를 래핑한 ResponseEntity
     */
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

    /**
     * 특정 멤버의 가입 신청서를 조회하여 반환합니다.
     *
     * 요청된 멤버 ID에 해당하는 가입 신청 정보(SpecifiedMemberResponse)를 조회하고, 성공 시
     * 표준 ApiResponse로 감싸서 200 OK 응답을 반환합니다.
     *
     * 관리자 권한(ROLE_ADMIN)이 필요합니다.
     *
     * @param memberId 조회할 멤버의 고유 ID
     * @return 조회된 가입 신청 정보를 담은 ApiResponse를 포함한 ResponseEntity
     */
    @Operation(summary = "특정 멤버 가입 신청서 조회", security = { @SecurityRequirement(name = "BearerAuth") })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/recruit/members/{memberId}")
    public ResponseEntity<ApiResponse<SpecifiedMemberResponse, Void>> getSpecifiedMember(
            @PathVariable Long memberId
    ) {
        SpecifiedMemberResponse response = recruitMemberService.findSpecifiedMember(memberId);

        return ResponseEntity.ok(ApiResponse.ok(MEMBER_RETRIEVED_SUCCESS, response));
    }

    // TODO 전체 응답 조회 및 검색

    // TODO 입금 완료

    // TODO 입금 미완료
}
