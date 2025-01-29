package inha.gdgoc.domain.recruit.enums;

import lombok.Getter;

@Getter
public enum Gender {
    MALE("남자 (Male)"),
    FEMALE("여자 (Female)");

    private final String gender;

    Gender(String geneder) {
        this.gender = geneder;
    }
}
