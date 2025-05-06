package inha.gdgoc.domain.study.enums;

import lombok.Getter;

@Getter
public enum CreatorType {
    GDGOC("GDGOC"),
    PERSONAL("PERSONAL");

    private final String value;

    CreatorType(String value) {
        this.value = value;
    }
}
