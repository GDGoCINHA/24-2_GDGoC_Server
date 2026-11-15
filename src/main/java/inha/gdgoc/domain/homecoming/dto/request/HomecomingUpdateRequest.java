package inha.gdgoc.domain.homecoming.dto.request;

import inha.gdgoc.domain.homecoming.enums.AttendType;
import inha.gdgoc.domain.homecoming.enums.HomecomingMemberRole;
import inha.gdgoc.domain.homecoming.enums.HomecomingTeam;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 운영진이 신청자의 변경 요청을 반영할 때 보내는 전체 값. 동의는 여기서 바꾸지 않는다(철회 API 를 쓴다). */
public record HomecomingUpdateRequest(
    @NotNull(message = "참석 여부를 선택해 주세요.") AttendType attendType,
    @NotBlank(message = "이름을 입력해 주세요.") @Size(max = 50) String name,
    @NotBlank(message = "전화번호 11자리를 입력해 주세요.")
        @Pattern(regexp = "^010-?\\d{4}-?\\d{4}$", message = "전화번호 11자리를 입력해 주세요.")
        String phone,
    Integer yearStart,
    Integer yearEnd,
    HomecomingMemberRole memberRole,
    HomecomingTeam team,
    Boolean isLate,
    String arrivalTime,
    @Size(max = 1000) String message) {}
