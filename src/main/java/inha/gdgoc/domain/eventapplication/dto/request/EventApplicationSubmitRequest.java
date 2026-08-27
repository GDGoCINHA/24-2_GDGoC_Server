package inha.gdgoc.domain.eventapplication.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 신청서 제출.
 *
 * <p>질문이 없는 행사면 answers 가 비어 있다. value 는 질문 유형에 따라 문자열·숫자·불린·배열로 온다.
 */
public record EventApplicationSubmitRequest(@Valid List<AnswerEntry> answers) {

  public record AnswerEntry(@NotNull Long questionId, Object value) {}
}
