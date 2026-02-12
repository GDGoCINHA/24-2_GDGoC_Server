package inha.gdgoc.domain.resource.service;

import inha.gdgoc.domain.auth.service.AuthService;
import inha.gdgoc.domain.resource.dto.response.S3ResultResponse;
import inha.gdgoc.domain.resource.enums.S3KeyType;
import inha.gdgoc.domain.resource.exception.ResourceErrorCode;
import inha.gdgoc.domain.resource.exception.ResourceException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final S3Service s3Service;
    private final AuthService authService;

    @Transactional
    public S3ResultResponse uploadImage(Authentication authentication, MultipartFile file, S3KeyType s3KeyType) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResourceException(ResourceErrorCode.INVALID_BIG_FILE);
        }

        Long userId = authService.getAuthenticationUserId(authentication);
        try {
            String savedS3Key = s3Service.upload(userId, s3KeyType, file);
            return new S3ResultResponse(savedS3Key);
        } catch (IOException e) {
            throw new ResourceException(ResourceErrorCode.RESOURCE_UPLOAD_FAILED);
        }
    }
}
