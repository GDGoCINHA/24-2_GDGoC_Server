package inha.gdgoc.domain.recruit.domain;

import lombok.Getter;

@Getter
enum Gender {
    MALE("남자 (Male)"),
    FEMALE("여자 (Female)"),
    ETC("기타 (Etc.)");

    private String gender;

    Gender(String geneder) {
        this.gender = geneder;
    }
}
