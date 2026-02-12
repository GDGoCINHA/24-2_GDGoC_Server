package inha.gdgoc.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginSuccessResponse {
    @JsonProperty("isNewUser")
    private boolean isNewUser;
    private String accessToken;
    private AuthUserResponse user;
    private String refreshToken;

    public static LoginSuccessResponse of(TokenDto tokens, AuthUserResponse user) {
        return LoginSuccessResponse.builder()
                .isNewUser(false)
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .user(user)
                .build();
    }
}
