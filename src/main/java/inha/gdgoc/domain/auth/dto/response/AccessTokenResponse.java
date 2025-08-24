package inha.gdgoc.domain.auth.dto.response;

import inha.gdgoc.global.common.BaseEntity;
import lombok.Getter;

@Getter
public class AccessTokenResponse extends BaseEntity {
    private final String access_token;

    public AccessTokenResponse(String accessToken) {
        this.access_token = accessToken;
    }
}
