package inha.gdgoc.domain.resource.service;

import inha.gdgoc.domain.resource.enums.S3KeyType;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public String upload(Long userId, S3KeyType s3key, MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        String key = "user/%d/%s/%s".formatted(userId, s3key.getValue(), fileName);

        PutObjectRequest putReq = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(file.getContentType())
            .build();

        s3Client.putObject(putReq, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        return key;
    }

    public String getS3FileUrl(String key) {
        return s3Client.utilities()
            .getUrl(GetUrlRequest.builder().bucket(bucketName).key(key).build())
            .toExternalForm();
    }
}
