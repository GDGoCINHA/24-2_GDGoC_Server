package inha.gdgoc.domain.club.completion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 최종 결과 제출.
 *
 * @param evidenceUrls 증빙 파일 URL. {@code S3KeyType.clubGoal} 로 올린 뒤의 공개 URL
 */
public record ClubGoalResultRequest(
    @NotBlank @Size(max = 5000) String goalResult,
    @Size(max = 10) List<@NotBlank @Size(max = 500) String> evidenceUrls) {}
