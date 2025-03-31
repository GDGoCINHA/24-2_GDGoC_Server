package inha.gdgoc.domain.auth.dto.response;

import inha.gdgoc.global.common.BaseEntity;

public class AccessTokenResponse extends BaseEntity {
    private final String accessToken;

    public AccessTokenResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
