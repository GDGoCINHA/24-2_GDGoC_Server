package inha.gdgoc.domain.eventapplication.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inha.gdgoc.domain.eventapplication.entity.EventFormQuestion;
import inha.gdgoc.domain.eventapplication.entity.QuestionOption;
import inha.gdgoc.domain.eventapplication.enums.QuestionType;
import inha.gdgoc.domain.eventapplication.exception.EventApplicationErrorCode;
import inha.gdgoc.global.exception.BusinessException;
import java.util.List;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EventFormValidatorTest {

  private final EventFormValidator validator = new EventFormValidator();

  @Test
  @DisplayName("선택형 질문에 선택지가 없으면 거절한다")
  void choiceRequiresOptions() {
    assertError(
        () -> validator.validateShape(QuestionType.SINGLE_CHOICE, null),
        EventApplicationErrorCode.OPTIONS_REQUIRED);
  }

  @Test
  @DisplayName("단답형에 선택지를 넣으면 거절한다")
  void shortTextRejectsOptions() {
    assertError(
        () -> validator.validateShape(QuestionType.SHORT_TEXT, options("A")),
        EventApplicationErrorCode.OPTIONS_NOT_ALLOWED);
  }

  @Test
  @DisplayName("선택지 값이 겹치면 거절한다")
  void duplicatedOptionValueRejected() {
    List<QuestionOption> duplicated =
        List.of(new QuestionOption("M", "미디엄"), new QuestionOption("M", "라지"));
    assertError(
        () -> validator.validateShape(QuestionType.DROPDOWN, duplicated),
        EventApplicationErrorCode.OPTION_VALUE_DUPLICATED);
  }

  @Test
  @DisplayName("뒤 순서 질문을 조건의 기준으로 삼으면 거절한다")
  void conditionMustPointBackward() {
    EventFormQuestion later = choice(2L, 5, "YES", "NO");

    assertError(
        () -> validator.validateCondition(1, 2L, List.of("YES"), List.of(later)),
        EventApplicationErrorCode.CONDITION_QUESTION_NOT_BEFORE);
  }

  @Test
  @DisplayName("선택형이 아닌 질문은 조건의 기준이 될 수 없다")
  void conditionBaseMustBeSelectable() {
    EventFormQuestion text = build(1L, 0, QuestionType.LONG_TEXT, null);

    assertError(
        () -> validator.validateCondition(1, 1L, List.of("아무값"), List.of(text)),
        EventApplicationErrorCode.CONDITION_QUESTION_TYPE_INVALID);
  }

  @Test
  @DisplayName("기준 질문의 선택지에 없는 값은 조건으로 쓸 수 없다")
  void conditionValueMustExistInOptions() {
    EventFormQuestion base = choice(1L, 0, "YES", "NO");

    assertError(
        () -> validator.validateCondition(1, 1L, List.of("MAYBE"), List.of(base)),
        EventApplicationErrorCode.CONDITION_VALUE_INVALID);
  }

  @Test
  @DisplayName("동의 질문은 true/false 만 조건 값으로 받는다")
  void agreementConditionAcceptsBooleanStrings() {
    EventFormQuestion base = build(1L, 0, QuestionType.AGREEMENT, null);

    assertThatCode(() -> validator.validateCondition(1, 1L, List.of("true"), List.of(base)))
        .doesNotThrowAnyException();
    assertError(
        () -> validator.validateCondition(1, 1L, List.of("YES"), List.of(base)),
        EventApplicationErrorCode.CONDITION_VALUE_INVALID);
  }

  @Test
  @DisplayName("조건 값이 비어 있으면 거절한다")
  void conditionValuesCannotBeEmpty() {
    EventFormQuestion base = choice(1L, 0, "YES", "NO");

    assertError(
        () -> validator.validateCondition(1, 1L, List.of(), List.of(base)),
        EventApplicationErrorCode.CONDITION_VALUES_EMPTY);
  }

  @Test
  @DisplayName("다른 질문이 기준으로 삼고 있으면 지울 수 없다")
  void referencedQuestionCannotBeRemoved() {
    EventFormQuestion base = choice(1L, 0, "YES", "NO");
    EventFormQuestion dependent = build(2L, 1, QuestionType.SHORT_TEXT, null);
    dependent.updateCondition(1L, List.of("YES"));

    assertError(
        () -> validator.validateNotReferenced(1L, List.of(base, dependent)),
        EventApplicationErrorCode.CONDITION_REFERENCED);
  }

  @Test
  @DisplayName("순서를 바꿔 기준 질문이 뒤로 가면 거절한다")
  void reorderCannotBreakConditions() {
    EventFormQuestion base = choice(1L, 0, "YES", "NO");
    EventFormQuestion dependent = build(2L, 1, QuestionType.SHORT_TEXT, null);
    dependent.updateCondition(1L, List.of("YES"));

    assertThatCode(() -> validator.validateOrderKeepsConditions(List.of(base, dependent)))
        .doesNotThrowAnyException();
    assertError(
        () -> validator.validateOrderKeepsConditions(List.of(dependent, base)),
        EventApplicationErrorCode.CONDITION_ORDER_BROKEN);
  }

  @Test
  @DisplayName("신청자가 있으면 선택지 값을 지울 수 없지만 라벨은 고칠 수 있다")
  void optionValuesAreKeptButLabelsMayChange() {
    List<QuestionOption> before = List.of(new QuestionOption("M", "M"), new QuestionOption("L", "L"));

    assertThatCode(
            () ->
                validator.validateOptionValuesKept(
                    before, List.of(new QuestionOption("M", "M (95)"), new QuestionOption("L", "L"))))
        .doesNotThrowAnyException();

    assertError(
        () -> validator.validateOptionValuesKept(before, options("M")),
        EventApplicationErrorCode.OPTION_VALUE_LOCKED);
  }

  private void assertError(Runnable action, EventApplicationErrorCode expected) {
    assertThatThrownBy(action::run)
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(expected);
  }

  private static List<QuestionOption> options(String... values) {
    return List.of(values).stream().map(v -> new QuestionOption(v, v)).toList();
  }

  private static EventFormQuestion choice(Long id, int sortOrder, String... values) {
    return build(id, sortOrder, QuestionType.SINGLE_CHOICE, options(values));
  }

  private static EventFormQuestion build(
      Long id, int sortOrder, QuestionType type, List<QuestionOption> options) {
    EventFormQuestion question =
        EventFormQuestion.create(null, type, "질문", null, false, sortOrder, options, null, null);
    ReflectionTestUtils.setField(question, "id", id);
    return question;
  }
}
