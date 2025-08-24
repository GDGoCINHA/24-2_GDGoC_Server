package inha.gdgoc.domain.recruit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckPhoneNumberRequest {
    @NotBlank(message = "전화번호는 필수 입력 값입니다.")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호 형식은 010-XXXX-XXXX 이어야 합니다.")
    private String phoneNumber;
}
