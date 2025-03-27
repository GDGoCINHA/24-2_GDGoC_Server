package inha.gdgoc.domain.auth.controller;

import inha.gdgoc.domain.auth.service.GoogleOAuthService;
import inha.gdgoc.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final GoogleOAuthService googleOAuthService;

    @GetMapping("/oauth2/google/callback")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleGoogleCallback(
            @RequestParam String code,
            HttpServletResponse response
    ) {
        Map<String, Object> data = googleOAuthService.processOAuthLogin(code, response);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

//    @PostMapping("/auth/token/refresh")
//    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(@RequestBody Map<String, String> body) {
//        String refreshToken = body.get("refreshToken");
//        // 유효성 검사 → DB에서 토큰 비교 → 새 AccessToken 발급
//        return ResponseEntity.ok(ApiResponse.success(null));
//    }

}
