package inha.gdgoc.domain.resource.controller;

import static inha.gdgoc.domain.resource.controller.message.ResourceMessage.IMAGE_SAVE_SUCCESS;

import inha.gdgoc.domain.auth.service.AuthService;
import inha.gdgoc.domain.resource.dto.response.S3ResultResponse;
import inha.gdgoc.domain.resource.enums.S3KeyType;
import inha.gdgoc.domain.resource.exception.ResourceErrorCode;
import inha.gdgoc.domain.resource.exception.ResourceException;
import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.global.dto.response.ApiResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/resource")
@RequiredArgsConstructor
public class ResourceController {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final S3Service s3Service;
    private final AuthService authService;

    // TODO 책임 분리
    @PostMapping("/image")
    public ResponseEntity<ApiResponse<S3ResultResponse, Void>> uploadImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam("s3key") S3KeyType s3key
    ) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResourceException(ResourceErrorCode.INVALID_BIG_FILE);
        }

        Long userId = authService.getAuthenticationUserId(authentication);
        try {
            String result_s3Key = s3Service.upload(userId, s3key, file);
            S3ResultResponse response = new S3ResultResponse(result_s3Key);

            return ResponseEntity.ok(ApiResponse.ok(IMAGE_SAVE_SUCCESS, response));
        } catch (IOException e) {
            throw new RuntimeException("s3 upload fail" + e);
        }
    }
}
