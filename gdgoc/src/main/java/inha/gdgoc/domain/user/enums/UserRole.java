package inha.gdgoc.domain.user.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    GUEST("Guest"),
    MEMBER("Member"),
    ADMIN("ADMIN");

    private final String core;

    UserRole(String core){
        this.core = core;
    }
}
