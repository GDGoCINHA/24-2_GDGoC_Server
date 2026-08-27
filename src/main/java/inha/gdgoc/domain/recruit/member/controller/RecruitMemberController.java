package inha.gdgoc.domain.recruit.member.controller;

import static inha.gdgoc.domain.recruit.member.controller.message.RecruitMemberMessage.EMAIL_DUPLICATION_CHECK_SUCCESS;
import static inha.gdgoc.domain.recruit.member.controller.message.RecruitMemberMessage.MEMBER_LIST_RETRIEVED_SUCCESS;
import static inha.gdgoc.domain.recruit.member.controller.message.RecruitMemberMessage.MEMBER_MEMO_SAVE_SUCCESS;
import static inha.gdgoc.domain.recruit.member.controller.message.RecruitMemberMessage.MEMBER_RETRIEVED_SUCCESS;
import static inha.gdgoc.domain.recruit.member.controller.message.RecruitMemberMessage.MEMBER_SAVE_SUCCESS;
import static inha.gdgoc.domain.recruit.member.controller.message.RecruitMemberMessage.PAYMENT_MARKED_COMPLETE_SUCCESS;
import static inha.gdgoc.domain.recruit.member.controller.message.RecruitMemberMessage.PAYMENT_MARKED_INCOMPLETE_SUCCESS;
import static inha.gdgoc.domain.recruit.member.controller.message.RecruitMemberMessage.PHONE_NUMBER_DUPLICATION_CHECK_SUCCESS;
import static inha.gdgoc.domain.recruit.member.controller.message.RecruitMemberMessage.STUDENT_ID_DUPLICATION_CHECK_SUCCESS;

import inha.gdgoc.domain.recruit.member.dto.request.CheckEmailRequest;
import inha.gdgoc.domain.recruit.member.dto.request.CheckPhoneNumberRequest;
import inha.gdgoc.domain.recruit.member.dto.request.CheckStudentIdRequest;
import inha.gdgoc.domain.recruit.member.dto.request.PaymentUpdateRequest;
import inha.gdgoc.domain.recruit.member.dto.request.RecruitMemberMemoRequest;
import inha.gdgoc.domain.recruit.member.dto.response.CheckEmailResponse;
import inha.gdgoc.domain.recruit.member.dto.response.CheckPhoneNumberResponse;
import inha.gdgoc.domain.recruit.member.dto.response.CheckStudentIdResponse;
import inha.gdgoc.domain.recruit.member.dto.response.RecruitMemberPeriodResponse;
import inha.gdgoc.domain.recruit.member.dto.response.RecruitMemberSummaryResponse;
import inha.gdgoc.domain.recruit.member.dto.response.SpecifiedMemberResponse;
import inha.gdgoc.domain.recruit.member.enums.AdmissionSemester;
import inha.gdgoc.domain.resource.dto.response.PresignedUploadResponse;
import inha.gdgoc.domain.recruit.member.entity.RecruitMember;
import inha.gdgoc.domain.recruit.member.service.RecruitMemberPeriodService;
import inha.gdgoc.domain.recruit.member.service.RecruitMemberService;
import inha.gdgoc.global.config.jwt.TokenProvider.CustomUserDetails;
import inha.gdgoc.global.dto.response.ApiResponse;
import inha.gdgoc.global.dto.response.PageMeta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import java.util.Map;

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
    private final RecruitMemberPeriodService recruitMemberPeriodService;

    public record ProofFilePresignedUploadRequest(
            @NotBlank String fileName,
            @NotBlank String contentType,
            @NotNull @PositiveOrZero Long fileSize
    ) {
    }

    /**
     * 부원 모집 기간. 코어의 {@code GET /api/v1/recruit/core/period} 와 같은 역할이다.
     *
     * <p>비로그인 방문자가 지원 화면에 들어오기 전에 본다. SecurityConfig 의 permitAll 에 넣어야 한다.
     */
    @GetMapping("/period")
    public ResponseEntity<RecruitMemberPeriodResponse> period() {
        return ResponseEntity.ok(recruitMemberPeriodService.getPeriod());
    }

    /**
     * 지원 접수. 로그인이 필요하다.
     *
     * <p>이름·학번·이메일·전화·학과는 계정에서 가져오므로 폼이 보내도 무시한다. 이미 지원한 사람은 화면이 폼을 그리기 전에
     * {@code GET /applications/me} 로 걸러낸다 — 다 채우고 제출한 뒤에 "중복" 을 보는 것은 사고로 읽힌다.
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/apply", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void, Void>> recruitMemberAdd(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestBody Map<String, Object> applicationRequest
    ) {
        recruitMemberPeriodService.validateOpen();
        recruitMemberService.addRecruitMember(applicationRequest, null, me.getUserId());

        return ResponseEntity.ok(ApiResponse.ok(MEMBER_SAVE_SUCCESS));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void, Void>> recruitMemberAddMultipart(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestPart("request") Map<String, Object> applicationRequest,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        recruitMemberPeriodService.validateOpen();
        recruitMemberService.addRecruitMember(applicationRequest, file, me.getUserId());

        return ResponseEntity.ok(ApiResponse.ok(MEMBER_SAVE_SUCCESS));
    }

    /** 증빙 파일도 지원의 일부다. 기간 밖에 업로드 URL 만 받아두는 길을 열어두지 않는다. */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/apply/proof-file/presigned-upload")
    public ResponseEntity<ApiResponse<PresignedUploadResponse, Void>> createProofFilePresignedUpload(
            @Valid @RequestBody ProofFilePresignedUploadRequest request
    ) {
        recruitMemberPeriodService.validateOpen();
        PresignedUploadResponse response = recruitMemberService.createProofFilePresignedUpload(
                request.fileName(),
                request.contentType(),
                request.fileSize()
        );

        return ResponseEntity.ok(ApiResponse.ok("증빙 파일 업로드 URL을 발급했습니다.", response));
    }

    @PostMapping("/memo")
    public ResponseEntity<ApiResponse<Void, Void>> recruitMemberMemoAdd(
            @Valid @RequestBody RecruitMemberMemoRequest recruitMemberMemoRequest
    ) {
        recruitMemberService.addRecruitMemberMemo(recruitMemberMemoRequest);
        return ResponseEntity.ok(ApiResponse.ok(MEMBER_MEMO_SAVE_SUCCESS));
    }

    @PostMapping("/check/student-id")
    public ResponseEntity<ApiResponse<CheckStudentIdResponse, Void>> duplicatedStudentIdDetails(
            @Valid @RequestBody CheckStudentIdRequest request
    ) {
        CheckStudentIdResponse response = recruitMemberService.isRegisteredStudentId(request.getStudentId());

        return ResponseEntity.ok(ApiResponse.ok(STUDENT_ID_DUPLICATION_CHECK_SUCCESS, response));
    }

    @PostMapping("/check/phone-number")
    public ResponseEntity<ApiResponse<CheckPhoneNumberResponse, Void>> duplicatedPhoneNumberDetails(
            @Valid @RequestBody CheckPhoneNumberRequest request
    ) {
        CheckPhoneNumberResponse response = recruitMemberService
                .isRegisteredPhoneNumber(request.getPhoneNumber());

        return ResponseEntity.ok(ApiResponse.ok(PHONE_NUMBER_DUPLICATION_CHECK_SUCCESS, response));
    }

    @PostMapping("/check/email")
    public ResponseEntity<ApiResponse<CheckEmailResponse, Void>> duplicatedEmailDetails(
            @Valid @RequestBody CheckEmailRequest request
    ) {
        CheckEmailResponse response = recruitMemberService.isRegisteredEmail(request.getEmail());

        return ResponseEntity.ok(ApiResponse.ok(EMAIL_DUPLICATION_CHECK_SUCCESS, response));
    }

    /**
     * 마이페이지에서 본인 지원서를 연다.
     *
     * <p>{@code /{memberId}} 보다 먼저 둔다. 세그먼트 수가 달라 충돌하지는 않지만 읽는 순서를 맞춘다.
     */
    @Operation(summary = "나의 부원 지원서 조회", security = {@SecurityRequirement(name = "BearerAuth")})
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/applications/me")
    public ResponseEntity<ApiResponse<SpecifiedMemberResponse, Void>> getMyApplication(
            @AuthenticationPrincipal CustomUserDetails me
    ) {
        SpecifiedMemberResponse response = recruitMemberService.findMyApplication(me.getUserId());

        return ResponseEntity.ok(ApiResponse.ok(MEMBER_RETRIEVED_SUCCESS, response));
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

            @Parameter(description = "지원 학기. 없으면 전체 조회", example = "Y26_2")
            @RequestParam(required = false) AdmissionSemester admissionSemester,

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

        Page<RecruitMember> memberPage =
                recruitMemberService.searchMembers(question, admissionSemester, pageable);

        List<RecruitMemberSummaryResponse> list = memberPage
                .map(RecruitMemberSummaryResponse::from)
                .getContent();
        PageMeta meta = PageMeta.of(memberPage);

        return ResponseEntity.ok(ApiResponse.ok(MEMBER_LIST_RETRIEVED_SUCCESS, list, meta));
    }

}
