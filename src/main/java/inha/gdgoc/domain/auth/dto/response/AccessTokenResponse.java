package inha.gdgoc.domain.auth.dto.response;

import inha.gdgoc.global.entity.BaseEntity;
import lombok.Getter;

@Getter
public class AccessTokenResponse extends BaseEntity {
    private final String accessToken;
    private final AuthUserResponse user;

    public AccessTokenResponse(String accessToken, AuthUserResponse user) {
        this.accessToken = accessToken;
        this.user = user;
    }
}
