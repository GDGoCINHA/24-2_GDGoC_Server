package inha.gdgoc.domain.recruit.member.enums;

import lombok.Getter;

@Getter
public enum InputType {
    APPLY_MOTIVATION("gdgUserMotive", "String"),
    LIFE_STORY("gdgUserStory", "String"),
    INTERESTS("gdgInterest", "List"),
    GDG_PERIOD("gdgPeriod", "List"),
    ROUTE_TO_KNOW("gdgRoute", "String"),
    WANT_TO_GET("gdgExpect", "String"),
    EXPECTED_ACTIVITY("gdgWish", "List"),
    FEEDBACK("gdgFeedback", "String");

    private final String question;
    private final String dataType;

    InputType(String question, String dataType) {
        this.question = question;
        this.dataType = dataType;
    }

    public static InputType fromQuestion(String question) {
        for (InputType inputType : InputType.values()) {
            if (inputType.question.equals(question)) {
                return inputType;
            }
        }
        throw new IllegalArgumentException("Invalid question value: " + question);
    }
}
