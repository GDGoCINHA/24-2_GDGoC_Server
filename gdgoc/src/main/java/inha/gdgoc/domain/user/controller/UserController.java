package inha.gdgoc.domain.user.controller;

import inha.gdgoc.domain.auth.dto.request.FindIdRequest;
import inha.gdgoc.domain.user.dto.request.UserSignupRequest;
import inha.gdgoc.domain.auth.dto.response.FindIdResponse;
import inha.gdgoc.domain.user.service.UserService;
import inha.gdgoc.global.common.ApiResponse;
import inha.gdgoc.global.common.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class UserController {

    private final UserService userService;

    // TODO 이메일 중복 조회

    @PostMapping("/auth/signup")
    public ResponseEntity<ApiResponse<String>> userSignup(
            @RequestBody UserSignupRequest userSignupRequest) {
        userService.saveUser(userSignupRequest);
        return ResponseEntity.ok(ApiResponse.of(null, null));
    }

    @PostMapping("/auth/findId")
    public ResponseEntity<?> findId(@RequestBody FindIdRequest findIdRequest) {
        try {
            FindIdResponse response = userService.findId(findIdRequest);
            return ResponseEntity.ok(ApiResponse.of(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
}
