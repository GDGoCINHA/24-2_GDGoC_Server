package inha.gdgoc.domain.recruit.controller;

import static inha.gdgoc.domain.recruit.controller.message.RecruitMemberMessage.MEMBER_RETRIEVED_SUCCESS;
import static inha.gdgoc.domain.recruit.controller.message.RecruitMemberMessage.MEMBER_SAVE_SUCCESS;
import static inha.gdgoc.domain.recruit.controller.message.RecruitMemberMessage.PHONE_NUMBER_DUPLICATION_CHECK_SUCCESS;
import static inha.gdgoc.domain.recruit.controller.message.RecruitMemberMessage.STUDENT_ID_DUPLICATION_CHECK_SUCCESS;

import inha.gdgoc.domain.recruit.dto.request.ApplicationRequest;
import inha.gdgoc.domain.recruit.dto.request.CheckPhoneNumberRequest;
import inha.gdgoc.domain.recruit.dto.response.CheckStudentIdResponse;
import inha.gdgoc.domain.recruit.dto.response.SpecifiedMemberResponse;
import inha.gdgoc.domain.recruit.service.RecruitMemberService;
import inha.gdgoc.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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


    // TODO DTO로 응답 리팩토링
    @GetMapping("/check/phoneNumber")
    public ResponseEntity<ApiResponse<Boolean, Void>> duplicatedPhoneNumberDetails(
            @Valid @ModelAttribute CheckPhoneNumberRequest phoneNumberRequest
    ) {
        boolean exists = recruitMemberService.isRegisteredPhoneNumber(phoneNumberRequest.getPhoneNumber());

        return ResponseEntity.ok(ApiResponse.ok(PHONE_NUMBER_DUPLICATION_CHECK_SUCCESS, exists));
    }

    // TODO 코어 멤버 인증 리팩토링 (Authentication), requestparam으로 변경하기
    @GetMapping("/recruit/member")
    public ResponseEntity<ApiResponse<SpecifiedMemberResponse, Void>> getSpecifiedMember (
            @RequestParam Long userId
    ) {
        SpecifiedMemberResponse response = recruitMemberService.findSpecifiedMember(userId);

        return ResponseEntity.ok(ApiResponse.ok(MEMBER_RETRIEVED_SUCCESS, response));
    }

    // TODO 전체 응답 조회 및 검색

    // TODO 입금 완료

    // TODO 입금 미완료
}
