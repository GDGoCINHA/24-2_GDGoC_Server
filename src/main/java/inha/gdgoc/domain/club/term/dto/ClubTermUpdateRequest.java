package inha.gdgoc.domain.club.term.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** 기수 수정. null 인 항목은 그대로 둔다. */
public record ClubTermUpdateRequest(
    @Size(max = 100) @Pattern(regexp = ".*\\S.*", message = "이름은 비울 수 없습니다.") String name,
    @DecimalMin("0.01") @DecimalMax("1.00") @Digits(integer = 1, fraction = 2)
        BigDecimal attendanceRatio) {}
