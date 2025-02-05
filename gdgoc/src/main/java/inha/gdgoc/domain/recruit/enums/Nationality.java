package inha.gdgoc.domain.recruit.enums;

import lombok.Getter;

@Getter
public enum Nationality {
    SOUTH_KOREA("대한민국"),
    ETC("기타");

    private final String nation;

    Nationality(String nation) {
        this.nation = nation;
    }

    public static Nationality fromNation(String nation) {
        for (Nationality nationality : Nationality.values()) {
            if(nationality.nation.equals(nation)) {
                return nationality;
            }
        }
        throw new IllegalArgumentException("Invalid nationality value: " + nation);
    }
}
