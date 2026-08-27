package inha.gdgoc.domain.eventapplication.service;

import static inha.gdgoc.domain.eventapplication.exception.EventApplicationErrorCode.*;

import inha.gdgoc.domain.eventapplication.entity.EventFormQuestion;
import inha.gdgoc.domain.eventapplication.entity.QuestionOption;
import inha.gdgoc.global.exception.BusinessException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 신청서에 딸려온 답변을 검사하고, 실제로 저장할 답변만 골라낸다.
 *
 * <p>화면이 "이건 숨겼으니 검사하지 마세요"라고 하는 것을 믿지 않는다. 서버가 받은 답변으로 조건을 직접 평가해 보이는 질문을 정하고, 그 결과로 필수를 검사한다. 숨겨진
 * 질문에 딸려온 답은 400 을 내지 않고 조용히 버린다 — 사용자가 선택을 바꾸는 과정에서 자연스럽게 생기는 값이라 오류로 다룰 일이 아니다.
 */
@Component
public class AnswerValidator {

  /**
   * @param questions 살아 있는 질문을 sortOrder 오름차순으로
   * @param submitted 질문 id → 제출된 값
   * @return 저장할 답변만 남긴 맵. 숨겨진 질문은 빠져 있다
   */
  public Map<Long, Object> validateAndFilter(
      List<EventFormQuestion> questions, Map<Long, Object> submitted) {
    Set<Long> known = new HashSet<>();
    for (EventFormQuestion question : questions) {
      known.add(question.getId());
    }
    for (Long questionId : submitted.keySet()) {
      if (!known.contains(questionId)) {
        throw new BusinessException(ANSWER_QUESTION_UNKNOWN);
      }
    }

    Set<Long> visible = QuestionConditionEvaluator.visibleQuestionIds(questions, submitted);

    Map<Long, Object> kept = new LinkedHashMap<>();
    for (EventFormQuestion question : questions) {
      if (!visible.contains(question.getId())) {
        continue; // 숨겨진 질문의 답은 버린다.
      }
      Object value = submitted.get(question.getId());
      if (isEmpty(value)) {
        if (question.isRequired()) {
          throw new BusinessException(ANSWER_REQUIRED);
        }
        continue;
      }
      validateShape(question, value);
      kept.put(question.getId(), value);
    }
    return kept;
  }

  private boolean isEmpty(Object value) {
    if (value == null) {
      return true;
    }
    if (value instanceof String s) {
      return s.isBlank();
    }
    if (value instanceof Collection<?> c) {
      return c.isEmpty();
    }
    return false;
  }

  private void validateShape(EventFormQuestion question, Object value) {
    switch (question.getType()) {
      case SHORT_TEXT, LONG_TEXT -> requireString(value);
      case NUMBER -> {
        if (!(value instanceof Number)) {
          throw new BusinessException(ANSWER_TYPE_INVALID);
        }
      }
      case DATE -> requireIsoDate(value);
      case AGREEMENT -> {
        if (!(value instanceof Boolean)) {
          throw new BusinessException(ANSWER_TYPE_INVALID);
        }
      }
      case SINGLE_CHOICE, DROPDOWN -> {
        String selected = requireString(value);
        if (!allowedValues(question).contains(selected)) {
          throw new BusinessException(ANSWER_VALUE_INVALID);
        }
      }
      case MULTI_CHOICE -> {
        if (!(value instanceof Collection<?> selected)) {
          throw new BusinessException(ANSWER_TYPE_INVALID);
        }
        Set<String> allowed = allowedValues(question);
        for (Object item : selected) {
          if (!(item instanceof String s)) {
            throw new BusinessException(ANSWER_TYPE_INVALID);
          }
          if (!allowed.contains(s)) {
            throw new BusinessException(ANSWER_VALUE_INVALID);
          }
        }
      }
      // 파일은 업로드가 끝난 S3 키를 받는다. 키의 실재 확인은 저장 단계에서 한다.
      case FILE -> {
        if (!(value instanceof Map<?, ?>) && !(value instanceof String)) {
          throw new BusinessException(ANSWER_TYPE_INVALID);
        }
      }
    }
  }

  private String requireString(Object value) {
    if (!(value instanceof String s)) {
      throw new BusinessException(ANSWER_TYPE_INVALID);
    }
    return s;
  }

  private void requireIsoDate(Object value) {
    String raw = requireString(value);
    try {
      LocalDate.parse(raw);
    } catch (DateTimeParseException e) {
      throw new BusinessException(ANSWER_TYPE_INVALID);
    }
  }

  private Set<String> allowedValues(EventFormQuestion question) {
    Set<String> values = new HashSet<>();
    List<QuestionOption> options = question.getOptions();
    if (options != null) {
      for (QuestionOption option : options) {
        if (option != null && option.value() != null) {
          values.add(option.value());
        }
      }
    }
    return values;
  }
}
