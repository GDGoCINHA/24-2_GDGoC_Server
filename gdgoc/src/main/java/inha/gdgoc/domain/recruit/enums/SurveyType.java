package inha.gdgoc.domain.recruit.enums;

import lombok.Getter;

@Getter
public enum SurveyType {
    RECRUIT("recruit form"),
    PROJECT("project"),
    STUDY("study"),
    COFFEE_CHAT("coffee_chat");

    private final String type;

    SurveyType(String type){
        this.type = type;
    }
}
