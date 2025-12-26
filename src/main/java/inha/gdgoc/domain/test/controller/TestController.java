package inha.gdgoc.domain.test.controller;

import inha.gdgoc.global.dto.response.ApiResponse;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @GetMapping("/login_test")
    public ResponseEntity<ApiResponse<Map<String, Object>, Void>> loginTest(
        @CookieValue(value = "refresh_token", required = false) String refreshToken,
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        boolean hasRefreshToken = refreshToken != null && !refreshToken.isBlank();
        boolean hasAuthorization = authorization != null && !authorization.isBlank();

        Map<String, Object> data = Map.of(
            "has_refresh_token", hasRefreshToken,
            "has_authorization", hasAuthorization
        );

        return ResponseEntity.ok(ApiResponse.ok("LOGIN_TEST_OK", data));
    }
}
