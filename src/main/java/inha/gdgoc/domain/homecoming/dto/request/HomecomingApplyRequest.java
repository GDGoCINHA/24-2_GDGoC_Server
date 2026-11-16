package inha.gdgoc.domain.homecoming.dto.request;

import inha.gdgoc.domain.homecoming.enums.AttendType;
import inha.gdgoc.domain.homecoming.enums.HomecomingMemberRole;
import inha.gdgoc.domain.homecoming.enums.HomecomingTeam;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 초대장 신청 시트의 제출값.
 *
 * <p>여기서는 모양만 본다. 참석 유형에 따른 필수·비움 규칙은 {@code HomecomingProfileNormalizer} 가 판정한다 — 관리자 수정도 같은 규칙을 쓰기 때문이다.
 *
 * @param requestId 신청 시트를 열 때 만든 UUID. 응답 유실 후 재시도해도 같은 신청으로 처리하는 키다
 * @param consentVersion 신청자가 본 동의 안내문 버전
 */
public record HomecomingApplyRequest(
    @NotBlank(message = "요청 식별자가 없습니다.")
        @Pattern(regexp = "^[A-Za-z0-9-]{8,64}$", message = "요청 식별자가 올바르지 않습니다.")
        String requestId,
    @NotNull(message = "참석 여부를 선택해 주세요.") AttendType attendType,
    @NotBlank(message = "이름을 입력해 주세요.") @Size(max = 50, message = "이름은 50자까지 입력할 수 있어요.") String name,
    @NotBlank(message = "전화번호 11자리를 입력해 주세요.")
        @Pattern(regexp = "^010-?\\d{4}-?\\d{4}$", message = "전화번호 11자리를 입력해 주세요.")
        String phone,
    Integer yearStart,
    Integer yearEnd,
    HomecomingMemberRole memberRole,
    HomecomingTeam team,
    Boolean isLate,
    String arrivalTime,
    @Size(max = 1000, message = "문의·요청사항은 1,000자까지 입력할 수 있어요.") String message,
    boolean privacyAgreed,
    boolean eventInviteAgreed,
    boolean talentPoolAgreed,
    @NotBlank(message = "동의 안내문 버전이 없습니다.") @Size(max = 40) String consentVersion) {}
