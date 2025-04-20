package inha.gdgoc.domain.study.enums;

import lombok.Getter;

@Getter
public enum CreaterType {
    GDGOC("GDGOC"),
    PERSONAL("PERSONAL");

    private final String value;

    CreaterType(String value) {
        this.value = value;
    }
}
