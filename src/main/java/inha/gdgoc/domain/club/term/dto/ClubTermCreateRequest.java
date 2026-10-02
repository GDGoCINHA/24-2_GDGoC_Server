package inha.gdgoc.domain.club.term.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 기수 생성. 비율은 {@code numeric(3,2)} 에 들어가야 하므로 0.01 ~ 1.00, 소수 둘째 자리까지만 받는다.
 *
 * @param attendanceRatio 참석 비율 (0.5 = 50%)
 */
public record ClubTermCreateRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull @DecimalMin("0.01") @DecimalMax("1.00") @Digits(integer = 1, fraction = 2)
        BigDecimal attendanceRatio) {}
