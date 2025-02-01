package inha.gdgoc.domain.recruit.enums;

import lombok.Getter;

@Getter
public enum InputType {
    APPLY_MOTIVATION("apply motivation", "String"),
    LIFE_STORY("life story", "String"),
    INTERESTS("interests", "List"),
    GDG_PERIOD("GDG period", "List"),
    ROUTE_TO_KNOW("route to know", "String"),
    WANT_TO_GET("want to get", "String"),
    EXPECTED_ACTIVITY("expected activity", "String"),
    FEEDBACK("feedback", "String");

    private final String question;
    private final String dataType;

    InputType(String question, String dataType) {
        this.question = question;
        this.dataType = dataType;
    }
}
