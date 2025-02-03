package inha.gdgoc.domain.recruit.enums;

import lombok.Getter;

@Getter
public enum Gender {
    MALE("남자 (Male)"),
    FEMALE("여자 (Female)");

    private final String type;

    Gender(String type) {
        this.type = type;
    }

    public static Gender fromType(String type) {
        for (Gender gender : Gender.values()) {
            if(gender.type.equals(type)){
                return gender;
            }
        }
        throw new IllegalArgumentException("Invalid gender value: " + type);
    }
}
