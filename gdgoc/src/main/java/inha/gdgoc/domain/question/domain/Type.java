package inha.gdgoc.domain.question.domain;

import lombok.Getter;

@Getter
public enum Type {
    RECRUIT("Recruit"),
    PROJECT("Project"),
    STUDY("Study");

    private final String type;

    Type(String type) {
        this.type = type;
    }
}
