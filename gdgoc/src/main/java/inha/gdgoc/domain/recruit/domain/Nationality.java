package inha.gdgoc.domain.recruit.domain;

import lombok.Getter;

@Getter
enum Nationality {
    SOUTH_KOREA("대한민국"),
    ETC("기타");

    private final String nation;

    Nationality(String nation) {
        this.nation = nation;
    }
}
