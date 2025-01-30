package inha.gdgoc.domain.question.dto.response;

import inha.gdgoc.domain.question.entity.Question;
import inha.gdgoc.domain.question.enums.InputType;

public class QuestionResponse {
    private final Long id;
    private final int order;
    private final String title;
    private final String description;
    private final boolean isRequired;
    private final InputType inputType;

    public QuestionResponse(Question question) {
        this.id = question.getId();
        this.order = question.getOrder();
        this.title = question.getTitle();
        this.description = question.getDescription();
        this.isRequired = question.isRequired();
        this.inputType = question.getInputType();
    }
}
