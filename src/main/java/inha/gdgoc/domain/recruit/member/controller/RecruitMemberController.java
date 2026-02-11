package inha.gdgoc.domain.recruit.member.controller;

import static inha.gdgoc.domain.recruit.member.controller.message.RecruitMemberMessage.EMAIL_DUPLICATION_CHECK_SUCCESS;
import static inha.gdgoc.domain.recruit.member.controller.message.RecruitMemberMessage.MEMBER_LIST_RETRIEVED_SUCCESS;
import static inha.gdgoc.domain.recruit.member.controller.message.RecruitMemberMessage.MEMBER_RETRIEVED_SUCCESS;
import static inha.gdgoc.domain.recruit.member.controller.message.RecruitMemberMessage.MEMBER_SAVE_SUCCESS;
import static inha.gdgoc.domain.recruit.member.controller.message.RecruitMemberMessage.PAYMENT_MARKED_COMPLETE_SUCCESS;
import static inha.gdgoc.domain.recruit.member.controller.message.RecruitMemberMessage.PAYMENT_MARKED_INCOMPLETE_SUCCESS;
import static inha.gdgoc.domain.recruit.member.controller.message.RecruitMemberMessage.PHONE_NUMBER_DUPLICATION_CHECK_SUCCESS;
import static inha.gdgoc.domain.recruit.member.controller.message.RecruitMemberMessage.STUDENT_ID_DUPLICATION_CHECK_SUCCESS;

import inha.gdgoc.domain.recruit.member.dto.request.ApplicationRequest;
import inha.gdgoc.domain.recruit.member.dto.request.PaymentUpdateRequest;
import inha.gdgoc.domain.recruit.member.dto.response.CheckEmailResponse;
import inha.gdgoc.domain.recruit.member.dto.response.CheckPhoneNumberResponse;
import inha.gdgoc.domain.recruit.member.dto.response.CheckStudentIdResponse;
import inha.gdgoc.domain.recruit.member.dto.response.RecruitMemberSummaryResponse;
import inha.gdgoc.domain.recruit.member.dto.response.SpecifiedMemberResponse;
import inha.gdgoc.domain.recruit.member.entity.RecruitMember;
import inha.gdgoc.domain.recruit.member.service.RecruitMemberService;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.dto.response.PageMeta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Recruit - Members", description = "리크루팅 지원자 관리 API")
@RequestMapping("/api/v1/recruit/member")
@RequiredArgsConstructor
@RestController
public class RecruitMemberController {

    private static final String LEAD_OR_HR_RULE =
            "@accessGuard.check(authentication,"
                    + " T(inha.gdgoc.global.security.AccessGuard$AccessCondition).atLeast("
                    + "T(inha.gdgoc.domain.user.enums.UserRole).LEAD),"
                    + " T(inha.gdgoc.global.security.AccessGuard$AccessCondition).of("
                    + "T(inha.gdgoc.domain.user.enums.UserRole).CORE,"
                    + " T(inha.gdgoc.domain.user.enums.TeamType).HR))";

    private final RecruitMemberService recruitMemberService;

    @PostMapping(value = "/apply", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void, Void>> recruitMemberAdd(
            @RequestBody ApplicationRequest applicationRequest
    ) {
        recruitMemberService.addRecruitMember(applicationRequest);

        return ResponseEntity.ok(ApiResponse.ok(MEMBER_SAVE_SUCCESS));
    }

    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void, Void>> recruitMemberAddMultipart(
            @RequestPart("request") ApplicationRequest applicationRequest,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        recruitMemberService.addRecruitMember(applicationRequest);

        return ResponseEntity.ok(ApiResponse.ok(MEMBER_SAVE_SUCCESS));
    }

    @GetMapping("/check/student-id")
    public ResponseEntity<ApiResponse<CheckStudentIdResponse, Void>> duplicatedStudentIdDetails(
            @RequestParam
            @NotBlank(message = "학번은 필수 입력 값입니다.")
            @Pattern(regexp = "^12[0-9]{6}$", message = "유효하지 않은 학번 값입니다.")
            String studentId
    ) {
        CheckStudentIdResponse response = recruitMemberService.isRegisteredStudentId(studentId);

        return ResponseEntity.ok(ApiResponse.ok(STUDENT_ID_DUPLICATION_CHECK_SUCCESS, response));
    }

    @GetMapping("/check/phone-number")
    public ResponseEntity<ApiResponse<CheckPhoneNumberResponse, Void>> duplicatedPhoneNumberDetails(
            @RequestParam
            @NotBlank(message = "전화번호는 필수 입력 값입니다.")
            @Pattern(regexp = "^010-?\\d{4}-?\\d{4}$", message = "전화번호 형식은 010-XXXX-XXXX 또는 010XXXXXXXX 이어야 합니다.")
            String phoneNumber
    ) {
        CheckPhoneNumberResponse response = recruitMemberService
                .isRegisteredPhoneNumber(phoneNumber);

        return ResponseEntity.ok(ApiResponse.ok(PHONE_NUMBER_DUPLICATION_CHECK_SUCCESS, response));
    }

    @GetMapping("/check/email")
    public ResponseEntity<ApiResponse<CheckEmailResponse, Void>> duplicatedEmailDetails(
            @RequestParam
            @NotBlank(message = "이메일은 필수 입력 값입니다.")
            @Pattern(regexp = "^[a-zA-Z0-9+-\\_.]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$", message = "유효하지 않은 이메일 형식입니다.")
            String email
    ) {
        CheckEmailResponse response = recruitMemberService.isRegisteredEmail(email);

        return ResponseEntity.ok(ApiResponse.ok(EMAIL_DUPLICATION_CHECK_SUCCESS, response));
    }

    @Operation(summary = "특정 멤버 가입 신청서 조회", security = {@SecurityRequirement(name = "BearerAuth")})
    @PreAuthorize(LEAD_OR_HR_RULE)
    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<SpecifiedMemberResponse, Void>> getSpecifiedMember(
            @PathVariable Long memberId
    ) {
        SpecifiedMemberResponse response = recruitMemberService.findSpecifiedMember(memberId);

        return ResponseEntity.ok(ApiResponse.ok(MEMBER_RETRIEVED_SUCCESS, response));
    }

    @Operation(
            summary = "입금 상태 변경",
            description = "설정하려는 상태(NOT 현재 상태)를 body에 보내주세요. true=입금 완료, false=입금 미완료",
            security = { @SecurityRequirement(name = "BearerAuth") }
    )
    @PreAuthorize(LEAD_OR_HR_RULE)
    @PatchMapping("/{memberId}/payment")
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

    @Operation(
            summary = "지원자 목록 조회",
            description = "전체 목록 또는 이름 검색 결과를 반환합니다. 검색어(question)를 주면 이름 포함 검색, 없으면 전체 조회. sort랑 dir은 example 값 그대로 코딩하는 것 추천...",
            security = { @SecurityRequirement(name = "BearerAuth") }
    )
    @PreAuthorize(LEAD_OR_HR_RULE)
    @GetMapping("")
    public ResponseEntity<ApiResponse<List<RecruitMemberSummaryResponse>, PageMeta>> getMembers(
            @Parameter(description = "검색어(이름 부분 일치). 없으면 전체 조회", example = "소연")
            @RequestParam(required = false) String question,

            @Parameter(description = "페이지(0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "정렬 필드", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sort,

            @Parameter(description = "정렬 방향 ASC/DESC", example = "DESC")
            @RequestParam(defaultValue = "DESC") String dir
    ) {
        Direction direction = "ASC".equalsIgnoreCase(dir) ? Direction.ASC : Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        Page<RecruitMember> memberPage = (question == null || question.isBlank())
                ? recruitMemberService.findAllMembersPage(pageable)
                : recruitMemberService.searchMembersByNamePage(question, pageable);

        List<RecruitMemberSummaryResponse> list = memberPage
                .map(RecruitMemberSummaryResponse::from)
                .getContent();
        PageMeta meta = PageMeta.of(memberPage);

        return ResponseEntity.ok(ApiResponse.ok(MEMBER_LIST_RETRIEVED_SUCCESS, list, meta));
    }

}
