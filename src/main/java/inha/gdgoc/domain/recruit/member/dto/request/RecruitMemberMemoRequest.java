package inha.gdgoc.domain.recruit.member.dto.request;

import inha.gdgoc.domain.recruit.member.entity.RecruitMemberMemo;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Locale;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecruitMemberMemoRequest {

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String name;

    @NotBlank(message = "전화번호는 필수 입력 값입니다.")
    @Pattern(regexp = "^010-?\\d{4}-?\\d{4}$", message = "전화번호 형식은 010-XXXX-XXXX 또는 010XXXXXXXX 이어야 합니다.")
    private String phoneNumber;

    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "유효하지 않은 이메일 형식입니다.")
    private String email;

    @AssertTrue(message = "개인정보 처리방침 동의는 필수입니다.")
    private Boolean privacyAgreement;

    @AssertTrue(message = "신입생 지원 알림 신청 동의는 필수입니다.")
    private Boolean freshmanMemoAgreement;

    public RecruitMemberMemo toEntity() {
        String normalizedPhone = phoneNumber.replaceAll("[^0-9]", "");
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        return RecruitMemberMemo.builder()
                .name(name.trim())
                .phoneNumber(normalizedPhone)
                .email(normalizedEmail)
                .privacyAgreement(Boolean.TRUE.equals(privacyAgreement))
                .freshmanMemoAgreement(Boolean.TRUE.equals(freshmanMemoAgreement))
                .build();
    }
}
