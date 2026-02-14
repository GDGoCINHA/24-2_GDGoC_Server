package inha.gdgoc.domain.recruit.member.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum InputType {
    // Recruit Member
    INTERESTS("gdgInterest", "List"),
    EXPECTED_ACTIVITY("gdgWish", "List"),
    FEEDBACK("gdgFeedback", "String"),
    PROOF_FILE("proofFileUrl", "String"),

    // Recruit Core
    CORE_MOTIVATION("motivation", "String"),
    CORE_WISH("wish", "String"),
    CORE_STRENGTHS("strengths", "String"),
    CORE_PLEDGE("pledge", "String"),
    CORE_FILE_URLS("fileUrls", "List"),

    // Legacy aliases (keep for existing DB rows / old payloads)
    @Deprecated APPLY_MOTIVATION("gdgUserMotive", "String"),
    @Deprecated LIFE_STORY("gdgUserStory", "String"),
    @Deprecated GDG_PERIOD("gdgPeriod", "List"),
    @Deprecated ROUTE_TO_KNOW("gdgRoute", "String"),
    @Deprecated WANT_TO_GET("gdgExpect", "String");

    private final String question;
    private final String dataType;
    private static final Map<String, InputType> LOOKUP = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(InputType::getQuestion, Function.identity(), (first, second) -> first));

    InputType(String question, String dataType) {
        this.question = question;
        this.dataType = dataType;
    }

    public static InputType fromQuestion(String question) {
        InputType inputType = LOOKUP.get(question);
        if (inputType != null) {
            return inputType;
        }
        throw new IllegalArgumentException("Invalid question value: " + question);
    }
}
