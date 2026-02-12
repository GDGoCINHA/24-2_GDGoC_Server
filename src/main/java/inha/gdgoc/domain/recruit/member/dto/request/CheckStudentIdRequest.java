package inha.gdgoc.domain.recruit.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckStudentIdRequest {

    @NotBlank(message = "학번은 필수 입력 값입니다.")
    @Pattern(regexp = "^12[0-9]{6}$", message = "유효하지 않은 학번 값입니다.")
    private String studentId;
}
