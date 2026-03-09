package inha.gdgoc.domain.game.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class Rythm8beatScoreRequest {

    @NotBlank
    @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
    private String phoneNumber;

    @NotBlank
    @Size(max = 20)
    private String nickname;

    @NotNull
    @Min(0)
    @Max(10000)
    private Integer score;

    private Integer stageReached;
}
