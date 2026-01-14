package inha.gdgoc.domain.recruit.member.enums;

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

    public static SurveyType fromType (String type) {
        for(SurveyType surveyType : SurveyType.values()) {
            if(surveyType.type.equals(type)) {
                return surveyType;
            }
        }
        throw new IllegalArgumentException("Invalid SurveyType value: " + type);
    }
}
