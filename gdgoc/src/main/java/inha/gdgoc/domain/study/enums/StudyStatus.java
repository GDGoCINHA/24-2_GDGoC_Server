package inha.gdgoc.domain.study.enums;

import lombok.Getter;

@Getter
public enum StudyStatus {

    RECRUITING("RECRUITING"),
    RECRUITED("RECRUITED"),
    CANCELED("CANCELED");

    private String value;

    StudyStatus(String value) {
        this.value = value;
    }

    public boolean isRecruiting() {
        return this == RECRUITING;
    }

    public boolean isRecruited() {
        return this == RECRUITED;
    }
}
