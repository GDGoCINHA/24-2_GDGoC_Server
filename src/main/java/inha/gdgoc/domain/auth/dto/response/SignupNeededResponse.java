package inha.gdgoc.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SignupNeededResponse {
    @JsonProperty("isNewUser")
    private boolean isNewUser;
    private String oauthSubject;
    private String email;
    private String name;
    private String picture;
}
