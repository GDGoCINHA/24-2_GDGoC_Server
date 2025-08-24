package inha.gdgoc.domain.auth.dto.request;

public record CodeVerificationRequest(String email, String code) {
}
