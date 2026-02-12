package inha.gdgoc.domain.user.controller;

import static inha.gdgoc.domain.user.controller.message.UserMessage.USER_EMAIL_DUPLICATION_RETRIEVED_SUCCESS;
import static inha.gdgoc.domain.user.controller.message.UserMessage.USER_EMAIL_RETRIEVED_SUCCESS;

import inha.gdgoc.domain.auth.dto.request.FindIdRequest;
import inha.gdgoc.domain.user.dto.request.CheckDuplicatedEmailRequest;
import inha.gdgoc.domain.auth.dto.response.FindIdResponse;
import inha.gdgoc.domain.user.dto.response.CheckDuplicatedEmailResponse;
import inha.gdgoc.domain.user.service.UserService;
import inha.gdgoc.global.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1")
@RequiredArgsConstructor
@RestController
public class UserController {

    private final UserService userService;

    // 이메일 중복 체크 
    @GetMapping("/auth/check")
    public ResponseEntity<ApiResponse<CheckDuplicatedEmailResponse, Void>> checkDuplicatedEmail(
            @RequestParam String email
    ) {
        CheckDuplicatedEmailResponse response =
                userService.isExistsByEmail(new CheckDuplicatedEmailRequest(email));

        return ResponseEntity.ok(ApiResponse.ok(USER_EMAIL_DUPLICATION_RETRIEVED_SUCCESS, response));
    }


    // 아이디(이메일) 찾기
    @PostMapping("/auth/findId")
    public ResponseEntity<ApiResponse<FindIdResponse, Void>> findEmail(
            @RequestBody FindIdRequest findIdRequest
    ) {
        FindIdResponse response = userService.findId(findIdRequest);

        return ResponseEntity.ok(ApiResponse.ok(USER_EMAIL_RETRIEVED_SUCCESS, response));
    }
}