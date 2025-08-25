package inha.gdgoc.domain.user.controller;

import inha.gdgoc.domain.auth.dto.request.FindIdRequest;
import inha.gdgoc.domain.user.dto.request.CheckDuplicatedEmailRequest;
import inha.gdgoc.domain.user.dto.request.UserSignupRequest;
import inha.gdgoc.domain.auth.dto.response.FindIdResponse;
import inha.gdgoc.domain.user.dto.response.CheckDuplicatedEmailResponse;
import inha.gdgoc.domain.user.service.UserService;
import inha.gdgoc.global.dto.response.ApiResponse;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class UserController {

    private final UserService userService;

    @GetMapping("/auth/check")
    public ResponseEntity<ApiResponse<CheckDuplicatedEmailResponse>> checkDuplicatedEmail(
            @RequestBody CheckDuplicatedEmailRequest checkDuplicatedEmailRequest) {
        return ResponseEntity.ok(ApiResponse.of(userService.isExistsByEmail(checkDuplicatedEmailRequest)));
    }

    @PostMapping("/auth/signup")
    public ResponseEntity<ApiResponse<String>> userSignup(
            @RequestBody UserSignupRequest userSignupRequest) throws NoSuchAlgorithmException, InvalidKeyException {
        userService.saveUser(userSignupRequest);
        return ResponseEntity.ok(ApiResponse.of(null, null));
    }

    @PostMapping("/auth/findId")
    public ResponseEntity<?> findId(@RequestBody FindIdRequest findIdRequest) {
        FindIdResponse response = userService.findId(findIdRequest);

        return ResponseEntity.ok(ApiResponse.of(response));

    }
}
