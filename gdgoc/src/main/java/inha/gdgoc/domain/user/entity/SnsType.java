package inha.gdgoc.domain.user.entity;

import lombok.Getter;

@Getter
enum SnsType {
    GITHUB("Github"),
    INSTAGRAM("Instagram"),
    LINKEDIN("Linkedin");

    private final String name;

    SnsType(String name) {
        this.name = name;
    }
}
