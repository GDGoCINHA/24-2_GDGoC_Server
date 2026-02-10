package inha.gdgoc.domain.recruit.member.enums;

import lombok.Getter;

@Getter
public enum Gender {
    MALE("남성"),
    FEMALE("여성"),
    PRIVATE("비공개");

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
