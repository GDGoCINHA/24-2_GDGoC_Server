package inha.gdgoc.domain.eventapplication.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인하지 않은 폰으로 QR 을 찍었을 때. 신청할 때의 학번·이름으로 신청을 찾는다.
 *
 * <p>계정으로 신청한 사람도 이 경로로 체크인할 수 있다 — 그때는 계정의 학번·이름과 대조한다.
 */
public record AnonymousCheckinRequest(
    @NotBlank String token, @NotBlank String studentId, @NotBlank String name) {}
