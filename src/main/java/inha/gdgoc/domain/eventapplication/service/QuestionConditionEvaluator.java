package inha.gdgoc.domain.eventapplication.service;

import inha.gdgoc.domain.eventapplication.entity.EventFormQuestion;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 답변을 놓고 각 질문이 화면에 보이는지 판정한다.
 *
 * <p>같은 판정이 프론트 렌더러에도 있다. 한쪽만 고치면 화면에는 보이는데 서버가 필수라고 거절하거나 그 반대가 되므로, 규칙을 바꿀 때는 반드시 양쪽을 함께 고치고 여기의
 * 테스트를 갱신한다.
 *
 * <p>기준 질문이 자기보다 앞 순서라는 규칙 덕분에 한 번 훑는 것으로 판정이 끝난다. 순환 참조는 애초에 만들어질 수 없다.
 */
public final class QuestionConditionEvaluator {

  private QuestionConditionEvaluator() {}

  /**
   * 보이는 질문의 id 를 돌려준다.
   *
   * @param questions 삭제되지 않은 질문을 sortOrder 오름차순으로 넘긴다
   * @param answers 질문 id → 답변 값. 문자열·불린·컬렉션을 받는다
   */
  public static Set<Long> visibleQuestionIds(
      List<EventFormQuestion> questions, Map<Long, Object> answers) {
    Set<Long> visible = new LinkedHashSet<>();
    Map<Long, EventFormQuestion> byId = new HashMap<>();
    for (EventFormQuestion question : questions) {
      byId.put(question.getId(), question);
    }

    for (EventFormQuestion question : questions) {
      if (isVisible(question, byId, visible, answers)) {
        visible.add(question.getId());
      }
    }
    return visible;
  }

  private static boolean isVisible(
      EventFormQuestion question,
      Map<Long, EventFormQuestion> byId,
      Set<Long> visibleSoFar,
      Map<Long, Object> answers) {
    if (!question.hasCondition()) {
      return true;
    }
    Long baseId = question.getVisibleWhenQuestionId();
    EventFormQuestion base = byId.get(baseId);
    if (base == null) {
      // 기준 질문이 지워졌다면 조건을 판정할 수 없다. 숨기는 쪽이 안전하다.
      return false;
    }
    // 기준 질문 자체가 숨겨져 있으면 답이 있을 수 없다.
    if (!visibleSoFar.contains(baseId)) {
      return false;
    }
    return matches(answers == null ? null : answers.get(baseId), question.getVisibleWhenValues());
  }

  /** 답변이 기대값 중 하나에 해당하는지. 다중선택은 고른 값 중 하나라도 걸리면 참이다. */
  static boolean matches(Object answer, List<String> expected) {
    if (answer == null || expected == null || expected.isEmpty()) {
      return false;
    }
    if (answer instanceof Collection<?> collection) {
      for (Object item : collection) {
        if (item != null && expected.contains(String.valueOf(item))) {
          return true;
        }
      }
      return false;
    }
    return expected.contains(String.valueOf(answer));
  }
}
