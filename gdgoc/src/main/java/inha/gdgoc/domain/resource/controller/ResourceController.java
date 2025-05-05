package inha.gdgoc.domain.resource.controller;

import inha.gdgoc.domain.auth.service.AuthService;
import inha.gdgoc.domain.resource.dto.response.S3ResultResponse;
import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final S3Service s3Service;
    private final AuthService authService;

    @PostMapping("/image")
    public ResponseEntity<ApiResponse<S3ResultResponse>> uploadImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam("s3key") String s3key
    ) {
        Long userId = authService.getAuthenticationUserId(authentication);
        try {
            String result_s3Key = s3Service.upload(userId, s3key, file);
            return ResponseEntity.ok(ApiResponse.of(new S3ResultResponse(result_s3Key)));
        } catch (IOException e) {
            throw new RuntimeException("s3 upload fail" + e);
        }
    }
}
