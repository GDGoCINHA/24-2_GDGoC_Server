package inha.gdgoc.domain.resource.service;

import inha.gdgoc.domain.resource.enums.S3KeyType;
import inha.gdgoc.global.config.s3.S3Properties;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    public String upload(Long userId, S3KeyType s3key, MultipartFile file) throws IOException {
        String key = buildKey(userId, s3key, file.getOriginalFilename());

        PutObjectRequest putReq = PutObjectRequest.builder()
            .bucket(s3Properties.getBucket())
            .key(key)
            .contentType(file.getContentType())
            .build();

        s3Client.putObject(putReq, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        return key;
    }

    public PresignedUpload createPresignedUpload(
        Long userId,
        S3KeyType s3key,
        String originalFileName,
        String contentType
    ) {
        String safeContentType = contentType == null || contentType.isBlank()
            ? "application/octet-stream"
            : contentType;
        String key = buildKey(userId, s3key, originalFileName);
        PutObjectRequest putReq = PutObjectRequest.builder()
            .bucket(s3Properties.getBucket())
            .key(key)
            .contentType(safeContentType)
            .build();
        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(5))
            .putObjectRequest(putReq)
            .build();
        PresignedPutObjectRequest presignedReq = s3Presigner.presignPutObject(presignReq);
        return new PresignedUpload(key, presignedReq.url().toExternalForm());
    }

    public String getS3FileUrl(String key) {
        return s3Client.utilities()
            .getUrl(GetUrlRequest.builder().bucket(s3Properties.getBucket()).key(key).build())
            .toExternalForm();
    }

    /**
     * 객체의 크기를 바이트로 반환한다. 키가 없으면 null.
     *
     * <p>presigned 업로드는 서버가 완료를 알 수 없으므로, 첨부를 저장하기 전에 이 호출로 실제 업로드 여부를 확인한다.
     */
    public Long getObjectSize(String key) {
        try {
            HeadObjectResponse res = s3Client.headObject(
                HeadObjectRequest.builder().bucket(s3Properties.getBucket()).key(key).build());
            return res.contentLength();
        } catch (NoSuchKeyException e) {
            return null;
        }
    }

    private String buildKey(Long userId, S3KeyType s3key, String originalFileName) {
        String safeFileName = originalFileName == null || originalFileName.isBlank()
            ? "file"
            : originalFileName.replaceAll("[\\\\/]", "_");
        String fileName = UUID.randomUUID() + "-" + safeFileName;
        return "user/%d/%s/%s".formatted(userId, s3key.getValue(), fileName);
    }

    public record PresignedUpload(String key, String uploadUrl) {
    }
}
