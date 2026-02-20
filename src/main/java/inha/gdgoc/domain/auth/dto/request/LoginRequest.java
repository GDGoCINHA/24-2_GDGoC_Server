package inha.gdgoc.domain.auth.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginRequest {
    private String idToken;
    private String adminId;
    private String password;
}
