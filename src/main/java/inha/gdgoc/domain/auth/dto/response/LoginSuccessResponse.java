package inha.gdgoc.domain.auth.dto.response;

import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.domain.user.dto.response.UserResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginSuccessResponse {
    private boolean isNewUser;
    private String accessToken;
    private String refreshToken;
    private UserResponse user;

    public static LoginSuccessResponse of(User user, TokenDto tokens) {
        return LoginSuccessResponse.builder()
                .isNewUser(false)
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .user(UserResponse.from(user))
                .build();
    }
}