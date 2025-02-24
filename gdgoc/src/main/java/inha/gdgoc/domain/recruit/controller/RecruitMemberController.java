package inha.gdgoc.domain.recruit.controller;

import inha.gdgoc.domain.recruit.dto.request.ApplicationRequest;
import inha.gdgoc.domain.recruit.service.RecruitMemberService;
import inha.gdgoc.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class RecruitMemberController {

    private final RecruitMemberService recruitMemberService;

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<ApplicationRequest>> applyMember(
            @RequestBody ApplicationRequest applicationRequest) {
        recruitMemberService.applyMember(applicationRequest);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
