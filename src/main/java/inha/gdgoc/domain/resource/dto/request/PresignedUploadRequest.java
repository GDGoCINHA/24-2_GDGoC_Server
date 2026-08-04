package inha.gdgoc.domain.resource.dto.request;

import inha.gdgoc.domain.resource.enums.S3KeyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PresignedUploadRequest(
    @NotBlank String fileName,
    @NotBlank String contentType,
    @NotNull @PositiveOrZero Long fileSize,
    @NotNull S3KeyType s3key
) {
}
