package inha.gdgoc.domain.eventapplication.service;

import static org.assertj.core.api.Assertions.assertThat;

import inha.gdgoc.domain.eventapplication.entity.EventFormQuestion;
import inha.gdgoc.domain.eventapplication.entity.QuestionOption;
import inha.gdgoc.domain.eventapplication.enums.QuestionType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 조건부 표시의 판정 규칙.
 *
 * <p>같은 판정이 프론트 렌더러에도 있다. 규칙을 바꿀 때는 양쪽을 함께 고치고 이 테스트를 갱신한다.
 */
class QuestionConditionEvaluatorTest {

  @Test
  @DisplayName("조건이 없는 질문은 답이 무엇이든 보인다")
  void questionsWithoutConditionAreAlwaysVisible() {
    EventFormQuestion q1 = choice(1L, 0, "저녁 참여", "YES", "NO");
    EventFormQuestion q2 = shortText(2L, 1);

    assertThat(QuestionConditionEvaluator.visibleQuestionIds(List.of(q1, q2), Map.of()))
        .containsExactly(1L, 2L);
  }

  @Test
  @DisplayName("기준 질문의 답이 조건 값과 같으면 보인다")
  void visibleWhenAnswerMatches() {
    EventFormQuestion q1 = choice(1L, 0, "저녁 참여", "YES", "NO");
    EventFormQuestion q2 = shortText(2L, 1);
    condition(q2, 1L, List.of("YES"));

    assertThat(QuestionConditionEvaluator.visibleQuestionIds(List.of(q1, q2), Map.of(1L, "YES")))
        .containsExactly(1L, 2L);
  }

  @Test
  @DisplayName("기준 질문의 답이 다르면 숨는다")
  void hiddenWhenAnswerDiffers() {
    EventFormQuestion q1 = choice(1L, 0, "저녁 참여", "YES", "NO");
    EventFormQuestion q2 = shortText(2L, 1);
    condition(q2, 1L, List.of("YES"));

    assertThat(QuestionConditionEvaluator.visibleQuestionIds(List.of(q1, q2), Map.of(1L, "NO")))
        .containsExactly(1L);
  }

  @Test
  @DisplayName("기준 질문에 아직 답하지 않았으면 숨는다")
  void hiddenWhenBaseUnanswered() {
    EventFormQuestion q1 = choice(1L, 0, "저녁 참여", "YES", "NO");
    EventFormQuestion q2 = shortText(2L, 1);
    condition(q2, 1L, List.of("YES"));

    assertThat(QuestionConditionEvaluator.visibleQuestionIds(List.of(q1, q2), Map.of()))
        .containsExactly(1L);
  }

  @Test
  @DisplayName("다중선택은 고른 값 중 하나라도 조건에 걸리면 보인다")
  void multiChoiceMatchesAnySelectedValue() {
    EventFormQuestion q1 = multiChoice(1L, 0, "관심 세션", "A", "B", "C");
    EventFormQuestion q2 = shortText(2L, 1);
    condition(q2, 1L, List.of("B"));

    assertThat(
            QuestionConditionEvaluator.visibleQuestionIds(
                List.of(q1, q2), Map.of(1L, List.of("A", "B"))))
        .containsExactly(1L, 2L);

    assertThat(
            QuestionConditionEvaluator.visibleQuestionIds(
                List.of(q1, q2), Map.of(1L, List.of("A", "C"))))
        .containsExactly(1L);
  }

  @Test
  @DisplayName("동의 질문은 true/false 를 조건 값으로 쓴다")
  void agreementUsesBooleanValue() {
    EventFormQuestion q1 = agreement(1L, 0);
    EventFormQuestion q2 = shortText(2L, 1);
    condition(q2, 1L, List.of("true"));

    assertThat(QuestionConditionEvaluator.visibleQuestionIds(List.of(q1, q2), Map.of(1L, true)))
        .containsExactly(1L, 2L);
    assertThat(QuestionConditionEvaluator.visibleQuestionIds(List.of(q1, q2), Map.of(1L, false)))
        .containsExactly(1L);
  }

  @Test
  @DisplayName("기준 질문이 숨겨져 있으면 거기 딸린 질문도 숨는다")
  void hiddenChainPropagates() {
    EventFormQuestion q1 = choice(1L, 0, "저녁 참여", "YES", "NO");
    EventFormQuestion q2 = choice(2L, 1, "메뉴", "한식", "양식");
    condition(q2, 1L, List.of("YES"));
    EventFormQuestion q3 = shortText(3L, 2);
    condition(q3, 2L, List.of("한식"));

    // q1 에 NO 라고 답하면 q2 가 숨고, q2 를 기준 삼는 q3 도 함께 숨어야 한다.
    // 답변 맵에 q2 값이 남아 있어도 마찬가지다.
    Map<Long, Object> answers = Map.of(1L, "NO", 2L, "한식");

    assertThat(QuestionConditionEvaluator.visibleQuestionIds(List.of(q1, q2, q3), answers))
        .containsExactly(1L);
  }

  @Test
  @DisplayName("기준 질문이 목록에 없으면 숨는다")
  void hiddenWhenBaseMissing() {
    EventFormQuestion q2 = shortText(2L, 1);
    condition(q2, 99L, List.of("YES"));

    assertThat(QuestionConditionEvaluator.visibleQuestionIds(List.of(q2), Map.of(99L, "YES")))
        .isEmpty();
  }

  private static EventFormQuestion shortText(Long id, int sortOrder) {
    return build(id, sortOrder, QuestionType.SHORT_TEXT, "질문", null);
  }

  private static EventFormQuestion choice(Long id, int sortOrder, String label, String... values) {
    return build(id, sortOrder, QuestionType.SINGLE_CHOICE, label, options(values));
  }

  private static EventFormQuestion multiChoice(
      Long id, int sortOrder, String label, String... values) {
    return build(id, sortOrder, QuestionType.MULTI_CHOICE, label, options(values));
  }

  private static EventFormQuestion agreement(Long id, int sortOrder) {
    return build(id, sortOrder, QuestionType.AGREEMENT, "개인정보 수집 동의", null);
  }

  private static List<QuestionOption> options(String... values) {
    return List.of(values).stream().map(v -> new QuestionOption(v, v)).toList();
  }

  private static EventFormQuestion build(
      Long id, int sortOrder, QuestionType type, String label, List<QuestionOption> options) {
    EventFormQuestion question =
        EventFormQuestion.create(null, type, label, null, false, sortOrder, options, null, null);
    ReflectionTestUtils.setField(question, "id", id);
    return question;
  }

  private static void condition(EventFormQuestion question, Long baseId, List<String> values) {
    question.updateCondition(baseId, values);
  }
}
