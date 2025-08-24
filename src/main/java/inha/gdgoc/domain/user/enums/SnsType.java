package inha.gdgoc.domain.user.enums;

import lombok.Getter;

@Getter
public enum SnsType {
    GITHUB("Github"),
    INSTAGRAM("Instagram"),
    LINKEDIN("Linkedin");

    private final String name;

    SnsType(String name) {
        this.name = name;
    }
}
