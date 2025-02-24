package inha.gdgoc.domain.recruit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckStudentIdRequest {
    @NotBlank(message = "학번은 필수 입력 값입니다.")
    @Pattern(regexp = "^[0-9]{8}$", message = "학번은 숫자로만 구성된 8자리여야 합니다.")
    private String studentId;
}
