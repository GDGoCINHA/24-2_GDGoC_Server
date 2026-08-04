package inha.gdgoc.domain.resource.dto.response;

public record PresignedUploadResponse(
    String key,
    String uploadUrl
) {
}
