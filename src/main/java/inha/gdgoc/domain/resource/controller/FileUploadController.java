package inha.gdgoc.domain.resource.controller;

import inha.gdgoc.domain.resource.enums.S3KeyType;
import inha.gdgoc.domain.resource.service.S3Service;
import inha.gdgoc.global.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/fileupload")
@RequiredArgsConstructor
public class FileUploadController {

    private final S3Service s3Service;

    private record UploadResponse(String url) {}

    @PostMapping
    public ResponseEntity<ApiResponse<UploadResponse, Void>> upload(
        @RequestParam("file") MultipartFile file
    ) throws Exception {
        String key = s3Service.upload(0L, S3KeyType.study, file);
        String url = s3Service.getS3FileUrl(key);
        return ResponseEntity.ok(ApiResponse.ok("OK", new UploadResponse(url)));
    }
}


