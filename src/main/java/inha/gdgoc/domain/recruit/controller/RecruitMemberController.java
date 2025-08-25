package inha.gdgoc.domain.recruit.controller;

import static inha.gdgoc.domain.recruit.controller.message.RecruitMemberMessage.MEMBER_RETRIEVED_SUCCESS;
import static inha.gdgoc.domain.recruit.controller.message.RecruitMemberMessage.MEMBER_SAVE_SUCCESS;
import static inha.gdgoc.domain.recruit.controller.message.RecruitMemberMessage.PHONE_NUMBER_DUPLICATION_CHECK_SUCCESS;
import static inha.gdgoc.domain.recruit.controller.message.RecruitMemberMessage.STUDENT_ID_DUPLICATION_CHECK_SUCCESS;

import inha.gdgoc.domain.recruit.dto.request.ApplicationRequest;
import inha.gdgoc.domain.recruit.dto.request.CheckPhoneNumberRequest;
import inha.gdgoc.domain.recruit.dto.request.CheckStudentIdRequest;
import inha.gdgoc.domain.recruit.dto.response.SpecifiedMemberResponse;
import inha.gdgoc.domain.recruit.service.RecruitMemberService;
import inha.gdgoc.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    // TODO valid 핸들러 추가
    // TODO DTO로 응답 리팩토링, requestparam으로 변경하기
    @GetMapping("/check/studentId")
    public ResponseEntity<ApiResponse<Boolean, Void>> duplicatedStudentIdDetails(
            @Valid @ModelAttribute CheckStudentIdRequest studentIdRequest
    ) {
        boolean exists = recruitMemberService.isRegisteredStudentId(studentIdRequest.getStudentId());

        return ResponseEntity.ok(ApiResponse.ok(STUDENT_ID_DUPLICATION_CHECK_SUCCESS, exists));
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
}
