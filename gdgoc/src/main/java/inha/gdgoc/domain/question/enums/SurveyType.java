package inha.gdgoc.domain.question.enums;

import lombok.Getter;

@Getter
public enum SurveyType {
    RECRUIT("Recruit"),
    PROJECT("Project"),
    STUDY("Study");

    private final String type;

    SurveyType(String type) {
        this.type = type;
    }
}
