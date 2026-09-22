package inha.gdgoc.domain.eventapplication.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 로그인 없이 내는 신청서.
 *
 * <p>계정이 없으므로 신원을 직접 받는다. 학번·연락처 형식은 회원가입과 같다. major 는 회원가입과 같은 학과 코드('CSE' 등)다.
 */
public record AnonymousApplicationRequest(
    @NotBlank @Size(max = 50) String name,
    @NotBlank @Pattern(regexp = "^12[0-9]{6}$", message = "유효하지 않은 학번 값입니다.") String studentId,
    @NotBlank @Size(max = 100) String major,
    @NotBlank
        @Pattern(
            regexp = "^010-?\\d{4}-?\\d{4}$",
            message = "전화번호 형식은 010-XXXX-XXXX 또는 010XXXXXXXX 이어야 합니다.")
        String phoneNumber,
    @AssertTrue(message = "개인정보 수집·이용에 동의해야 신청할 수 있습니다.") boolean privacyAgreed,
    @Valid List<EventApplicationSubmitRequest.AnswerEntry> answers) {}
