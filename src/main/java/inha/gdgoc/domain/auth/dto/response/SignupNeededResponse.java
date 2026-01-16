package inha.gdgoc.domain.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SignupNeededResponse {
    private boolean isNewUser;
    private String oauthSubject;
    private String email;
    private String name;
}