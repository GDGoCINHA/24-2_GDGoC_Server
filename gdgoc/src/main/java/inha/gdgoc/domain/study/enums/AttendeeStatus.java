package inha.gdgoc.domain.study.enums;

import lombok.Getter;

@Getter
public enum AttendeeStatus {
    REQUESTED("REQUESTED"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED");

    private final String value;

    AttendeeStatus(String value){
        this.value = value;
    }
}
