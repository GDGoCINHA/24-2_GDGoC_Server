package inha.gdgoc.domain.resource.controller;

import static inha.gdgoc.domain.resource.controller.message.ResourceMessage.IMAGE_SAVE_SUCCESS;

import inha.gdgoc.domain.resource.dto.request.PresignedUploadRequest;
import inha.gdgoc.domain.resource.dto.response.PresignedUploadResponse;
import inha.gdgoc.domain.resource.dto.response.S3ResultResponse;
import inha.gdgoc.domain.resource.enums.S3KeyType;
import inha.gdgoc.domain.resource.service.ResourceService;
import inha.gdgoc.global.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping("/image")
    public ResponseEntity<ApiResponse<S3ResultResponse, Void>> uploadImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam("s3key") S3KeyType s3key
    ) {
        S3ResultResponse response = resourceService.uploadImage(authentication, file, s3key);
        return ResponseEntity.ok(ApiResponse.ok(IMAGE_SAVE_SUCCESS, response));
    }

    @PostMapping("/presigned-upload")
    public ResponseEntity<ApiResponse<PresignedUploadResponse, Void>> createPresignedUpload(
        Authentication authentication,
        @Valid @RequestBody PresignedUploadRequest request
    ) {
        PresignedUploadResponse response = resourceService.createPresignedUpload(authentication, request);
        return ResponseEntity.ok(ApiResponse.ok("파일 업로드 URL을 발급했습니다.", response));
    }
}
