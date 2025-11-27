package inha.gdgoc.domain.manito.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 마니또 세션 생성 요청
 */
public record ManitoSessionCreateRequest(

        @NotBlank(message = "세션 코드는 필수입니다.")
        String code,

        @NotBlank(message = "세션 제목은 필수입니다.")
        String title
) { }