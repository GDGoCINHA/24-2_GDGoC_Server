package inha.gdgoc.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SignupRequest {
    @NotBlank
    private String oauthSubject;
    @NotBlank
    private String email;
    @NotBlank
    private String name;
    @NotBlank
    private String studentId;
    @NotBlank
    private String phoneNumber;
    @NotBlank
    private String major;
}