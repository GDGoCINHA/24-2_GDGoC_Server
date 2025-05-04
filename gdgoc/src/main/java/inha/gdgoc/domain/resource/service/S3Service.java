package inha.gdgoc.domain.resource.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    //TODO S3처리
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public String upload(
            Long userId,
            String s3key,
            MultipartFile file
    ) throws IOException {
        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        String prefix = "user/" + userId + "/";
        String uploadFilePath = prefix + s3key + "/" + fileName;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        amazonS3.putObject(bucketName, uploadFilePath, file.getInputStream(), metadata);
        return uploadFilePath;
    }

    public String getS3FileUrl(String key) {
        return amazonS3.getUrl(bucketName, key).toString();
    }
}
