package inha.gdgoc.domain.eventapplication.dto.response;

import inha.gdgoc.domain.eventapplication.entity.EventFormQuestion;
import inha.gdgoc.domain.eventapplication.entity.QuestionOption;
import inha.gdgoc.domain.eventapplication.enums.QuestionType;
import java.util.List;

public record QuestionResponse(
    Long id,
    QuestionType type,
    String label,
    String helpText,
    boolean isRequired,
    int sortOrder,
    List<QuestionOption> options,
    Long visibleWhenQuestionId,
    List<String> visibleWhenValues) {

  public static QuestionResponse from(EventFormQuestion question) {
    return new QuestionResponse(
        question.getId(),
        question.getType(),
        question.getLabel(),
        question.getHelpText(),
        question.isRequired(),
        question.getSortOrder(),
        question.getOptions(),
        question.getVisibleWhenQuestionId(),
        question.getVisibleWhenValues());
  }
}
