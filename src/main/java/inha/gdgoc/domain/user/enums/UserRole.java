package inha.gdgoc.domain.user.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    GUEST("GUEST"),
    MEMBER("MEMBER"),
    CORE("CORE"),
    LEAD("LEAD"),
    ORGANIZER("ORGANIZER"),
    ADMIN("ADMIN");

    private final String role;

    UserRole(String role) {
        this.role = role;
    }
}