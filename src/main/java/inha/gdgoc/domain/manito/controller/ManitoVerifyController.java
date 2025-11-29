package inha.gdgoc.domain.manito.controller;

import inha.gdgoc.domain.manito.dto.request.ManitoVerifyRequest;
import inha.gdgoc.domain.manito.dto.response.ManitoVerifyResponse;
import inha.gdgoc.domain.manito.entity.ManitoAssignment;
import inha.gdgoc.domain.manito.service.ManitoUserService;
import inha.gdgoc.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/manito")
@RequiredArgsConstructor
public class ManitoVerifyController {

    private final ManitoUserService manitoUserService;

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<ManitoVerifyResponse, Void>> verify(@Valid @RequestBody ManitoVerifyRequest request) {
        ManitoAssignment assignment = manitoUserService.verifyAndGetAssignment(request.sessionCode(), request.studentId(), request.pin());

        String cipher = assignment.getEncryptedManitto();
        String ownerName = assignment.getName();

        ManitoVerifyResponse response = new ManitoVerifyResponse(cipher, ownerName);

        return ResponseEntity.ok(ApiResponse.ok("마니또 정보 조회 성공", response));
    }
}