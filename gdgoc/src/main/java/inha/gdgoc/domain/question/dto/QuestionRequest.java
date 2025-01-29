package inha.gdgoc.domain.question.dto;

import inha.gdgoc.domain.question.entity.Question;
import inha.gdgoc.domain.question.enums.InputType;
import inha.gdgoc.domain.question.enums.SurveyType;
import lombok.Data;

@Data
public class QuestionRequest {
    private SurveyType surveyType;
    private InputType inputType;
    private int order;
    private String title;
    private String description;
    private boolean isRequired;
    private boolean isUsed;

    public Question toEntity() {
        return Question.builder()
                .surveyType(surveyType)
                .inputType(inputType)
                .order(order)
                .title(title)
                .description(description)
                .isRequired(isRequired)
                .isUsed(isUsed)
                .build();
    }
}
