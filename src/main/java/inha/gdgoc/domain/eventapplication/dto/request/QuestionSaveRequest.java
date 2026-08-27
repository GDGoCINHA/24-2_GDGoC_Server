package inha.gdgoc.domain.eventapplication.dto.request;

import inha.gdgoc.domain.eventapplication.entity.QuestionOption;
import inha.gdgoc.domain.eventapplication.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 질문 추가·수정.
 *
 * <p>visibleWhenQuestionId 를 비우면 조건이 없는 질문이다. 수정에서 조건을 없애려면 {@code clearCondition} 을 true 로 보낸다.
 */
public record QuestionSaveRequest(
    @NotNull QuestionType type,
    @NotBlank @Size(max = 255) String label,
    @Size(max = 500) String helpText,
    boolean isRequired,
    List<QuestionOption> options,
    Long visibleWhenQuestionId,
    List<String> visibleWhenValues,
    boolean clearCondition) {}
