package inha.gdgoc.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class GoogleUserInfo {
    private String sub;
    private String email;
    private String name;
    private String givenName;
    private String familyName;
}
