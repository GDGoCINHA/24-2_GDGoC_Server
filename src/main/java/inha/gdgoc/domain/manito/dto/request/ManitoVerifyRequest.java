package inha.gdgoc.domain.manito.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 마니또 확인 요청 DTO
 * - 로그인 없이 세션 코드 + 학번 + PIN으로 검증
 */
public record ManitoVerifyRequest(

        @NotBlank(message = "세션 코드는 필수입니다.")
        String sessionCode,

        @NotBlank(message = "학번은 필수입니다.")
        String studentId,

        @NotBlank(message = "PIN은 필수입니다.")
        @Size(min = 4, max = 4, message = "PIN은 4자리여야 합니다.")
        @Pattern(regexp = "\\d{4}", message = "PIN은 숫자 4자리여야 합니다.")
        String pin
) {
}