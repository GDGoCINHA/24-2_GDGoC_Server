package inha.gdgoc.domain.core.attendance.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateDateRequest {
    @NotBlank(message = "날짜는 YYYY-MM-DD 형식이어야 합니다.")
    private String date;
}