package inha.gdgoc.domain.recruit.controller;

import inha.gdgoc.domain.recruit.dto.request.ApplicationRequest;
import inha.gdgoc.domain.recruit.dto.request.CheckPhoneNumberRequest;
import inha.gdgoc.domain.recruit.dto.request.CheckStudentIdRequest;
import inha.gdgoc.domain.recruit.dto.response.SpecifiedMemberResponse;
import inha.gdgoc.domain.recruit.service.RecruitMemberService;
import inha.gdgoc.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
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
    public ResponseEntity<ApiResponse<ApplicationRequest>> recruitMemberAdd(
            @RequestBody ApplicationRequest applicationRequest) {
        recruitMemberService.addRecruitMember(applicationRequest);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/check/studentId")
    public ResponseEntity<ApiResponse<Boolean>> duplicatedStudentIdDetails(
            @Valid @ModelAttribute CheckStudentIdRequest studentIdRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.ok(ApiResponse.failure(true, "학번 형식에 맞지 않는 값입니다."));
        }

        boolean exists = recruitMemberService.isRegisteredStudentId(studentIdRequest.getStudentId());
        return ResponseEntity.ok(ApiResponse.success(exists, exists ? "이미 등록된 학번입니다." : "사용 가능한 학번입니다."));
    }

    @GetMapping("/check/phoneNumber")
    public ResponseEntity<ApiResponse<Boolean>> duplicatedPhoneNumberDetails(
            @Valid @ModelAttribute CheckPhoneNumberRequest phoneNumberRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.ok(ApiResponse.failure(true, "전화번호 형식에 맞지 않는 값입니다."));
        }

        boolean exists = recruitMemberService.isRegisteredPhoneNumber(phoneNumberRequest.getPhoneNumber());
        return ResponseEntity.ok(ApiResponse.success(exists, exists ? "이미 등록된 전화번호입니다." : "사용 가능한 전화번호입니다."));
    }

    @GetMapping("/recruit/member")
    public ResponseEntity<ApiResponse<SpecifiedMemberResponse>> getSpecifiedMember (
            Authentication authentication,
            @RequestParam Long userId
    ) {
        SpecifiedMemberResponse data = recruitMemberService.findSpecifiedMember(userId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
