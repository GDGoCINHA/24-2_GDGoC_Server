package inha.gdgoc.domain.eventapplication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inha.gdgoc.domain.eventapplication.entity.EventFormQuestion;
import inha.gdgoc.domain.eventapplication.entity.QuestionOption;
import inha.gdgoc.domain.eventapplication.enums.QuestionType;
import inha.gdgoc.domain.eventapplication.exception.EventApplicationErrorCode;
import inha.gdgoc.global.exception.BusinessException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AnswerValidatorTest {

  private final AnswerValidator validator = new AnswerValidator();

  @Test
  @DisplayName("필수 질문에 답하지 않으면 거절한다")
  void requiredAnswerMissing() {
    EventFormQuestion q = required(text(1L, 0));

    assertError(() -> validator.validateAndFilter(List.of(q), Map.of()), EventApplicationErrorCode.ANSWER_REQUIRED);
  }

  @Test
  @DisplayName("빈 문자열은 답하지 않은 것으로 본다")
  void blankIsTreatedAsMissing() {
    EventFormQuestion q = required(text(1L, 0));

    assertError(
        () -> validator.validateAndFilter(List.of(q), answers(1L, "   ")),
        EventApplicationErrorCode.ANSWER_REQUIRED);
  }

  @Test
  @DisplayName("조건에 걸려 숨겨진 필수 질문은 검사하지 않는다")
  void hiddenRequiredQuestionIsExempt() {
    EventFormQuestion base = choice(1L, 0, "YES", "NO");
    EventFormQuestion dependent = required(text(2L, 1));
    dependent.updateCondition(1L, List.of("YES"));

    // NO 를 골랐으므로 2번은 숨는다. 필수여도 통과해야 한다.
    Map<Long, Object> kept = validator.validateAndFilter(List.of(base, dependent), answers(1L, "NO"));

    assertThat(kept).containsOnlyKeys(1L);
  }

  @Test
  @DisplayName("숨겨진 질문에 답이 딸려와도 오류 없이 버린다")
  void hiddenAnswersAreDiscardedSilently() {
    EventFormQuestion base = choice(1L, 0, "YES", "NO");
    EventFormQuestion dependent = text(2L, 1);
    dependent.updateCondition(1L, List.of("YES"));

    Map<Long, Object> submitted = new LinkedHashMap<>();
    submitted.put(1L, "NO");
    submitted.put(2L, "숨겨진 뒤에도 남아 있던 값");

    Map<Long, Object> kept = validator.validateAndFilter(List.of(base, dependent), submitted);

    assertThat(kept).containsOnlyKeys(1L);
  }

  @Test
  @DisplayName("조건이 만족되면 그 질문의 필수가 다시 살아난다")
  void visibleRequiredQuestionIsEnforced() {
    EventFormQuestion base = choice(1L, 0, "YES", "NO");
    EventFormQuestion dependent = required(text(2L, 1));
    dependent.updateCondition(1L, List.of("YES"));

    assertError(
        () -> validator.validateAndFilter(List.of(base, dependent), answers(1L, "YES")),
        EventApplicationErrorCode.ANSWER_REQUIRED);
  }

  @Test
  @DisplayName("선택지에 없는 값은 거절한다")
  void valueMustBeInOptions() {
    EventFormQuestion q = choice(1L, 0, "YES", "NO");

    assertError(
        () -> validator.validateAndFilter(List.of(q), answers(1L, "MAYBE")),
        EventApplicationErrorCode.ANSWER_VALUE_INVALID);
  }

  @Test
  @DisplayName("숫자 질문에 문자열을 보내면 거절한다")
  void numberRejectsString() {
    EventFormQuestion q = build(1L, 0, QuestionType.NUMBER, null);

    assertError(
        () -> validator.validateAndFilter(List.of(q), answers(1L, "둘")),
        EventApplicationErrorCode.ANSWER_TYPE_INVALID);
  }

  @Test
  @DisplayName("날짜 질문은 ISO 형식만 받는다")
  void dateMustBeIso() {
    EventFormQuestion q = build(1L, 0, QuestionType.DATE, null);

    assertThat(validator.validateAndFilter(List.of(q), answers(1L, "2026-09-01"))).containsKey(1L);
    assertError(
        () -> validator.validateAndFilter(List.of(q), answers(1L, "2026년 9월 1일")),
        EventApplicationErrorCode.ANSWER_TYPE_INVALID);
  }

  @Test
  @DisplayName("다중선택은 고른 값이 모두 선택지에 있어야 한다")
  void multiChoiceValidatesEveryValue() {
    EventFormQuestion q = build(1L, 0, QuestionType.MULTI_CHOICE, options("A", "B", "C"));

    assertThat(validator.validateAndFilter(List.of(q), answers(1L, List.of("A", "C"))))
        .containsKey(1L);
    assertError(
        () -> validator.validateAndFilter(List.of(q), answers(1L, List.of("A", "Z"))),
        EventApplicationErrorCode.ANSWER_VALUE_INVALID);
  }

  @Test
  @DisplayName("폼에 없는 질문에 답하면 거절한다")
  void unknownQuestionRejected() {
    EventFormQuestion q = text(1L, 0);

    assertError(
        () -> validator.validateAndFilter(List.of(q), answers(99L, "값")),
        EventApplicationErrorCode.ANSWER_QUESTION_UNKNOWN);
  }

  private void assertError(Runnable action, EventApplicationErrorCode expected) {
    assertThatThrownBy(action::run)
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(expected);
  }

  private static Map<Long, Object> answers(Long questionId, Object value) {
    Map<Long, Object> map = new LinkedHashMap<>();
    map.put(questionId, value);
    return map;
  }

  private static List<QuestionOption> options(String... values) {
    return List.of(values).stream().map(v -> new QuestionOption(v, v)).toList();
  }

  private static EventFormQuestion text(Long id, int sortOrder) {
    return build(id, sortOrder, QuestionType.SHORT_TEXT, null);
  }

  private static EventFormQuestion choice(Long id, int sortOrder, String... values) {
    return build(id, sortOrder, QuestionType.SINGLE_CHOICE, options(values));
  }

  private static EventFormQuestion required(EventFormQuestion question) {
    question.updateContent(null, null, true);
    return question;
  }

  private static EventFormQuestion build(
      Long id, int sortOrder, QuestionType type, List<QuestionOption> options) {
    EventFormQuestion question =
        EventFormQuestion.create(null, type, "질문", null, false, sortOrder, options, null, null);
    ReflectionTestUtils.setField(question, "id", id);
    return question;
  }
}
