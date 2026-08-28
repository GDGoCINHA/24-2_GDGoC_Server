package inha.gdgoc.domain.eventapplication.service;

import static inha.gdgoc.domain.eventapplication.exception.EventApplicationErrorCode.*;

import inha.gdgoc.domain.eventapplication.entity.EventFormQuestion;
import inha.gdgoc.domain.eventapplication.entity.QuestionOption;
import inha.gdgoc.domain.eventapplication.enums.QuestionType;
import inha.gdgoc.global.exception.BusinessException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 질문과 조건이 성립하는지 검사한다.
 *
 * <p>신청이 한 건이라도 들어온 뒤에는 허용 범위가 좁아진다. 저장된 답변의 형태가 달라지거나 답변이 고아가 되는 변경을 막기 위해서다.
 */
@Component
public class EventFormValidator {

  /** 동의 질문은 선택지가 없다. 조건 값으로는 이 둘만 쓸 수 있다. */
  private static final Set<String> AGREEMENT_VALUES = Set.of("true", "false");

  /** 유형과 선택지가 맞물리는지 본다. */
  public void validateShape(QuestionType type, List<QuestionOption> options) {
    boolean hasOptions = options != null && !options.isEmpty();

    if (type.isRequiresOptions()) {
      if (!hasOptions) {
        throw new BusinessException(OPTIONS_REQUIRED);
      }
      Set<String> seen = new HashSet<>();
      for (QuestionOption option : options) {
        if (option == null
            || option.value() == null
            || option.value().isBlank()
            || option.label() == null
            || option.label().isBlank()) {
          throw new BusinessException(OPTION_VALUE_BLANK);
        }
        if (!seen.add(option.value())) {
          throw new BusinessException(OPTION_VALUE_DUPLICATED);
        }
      }
      return;
    }

    if (hasOptions) {
      throw new BusinessException(OPTIONS_NOT_ALLOWED);
    }
  }

  /**
   * 표시 조건이 성립하는지 본다.
   *
   * @param sortOrder 조건을 거는 질문의 순서
   * @param baseId 기준 질문 id. null 이면 조건이 없는 것이므로 통과한다
   * @param values 기준 질문의 답이 이 중 하나일 때 보인다
   * @param siblings 같은 폼의 살아 있는 질문들
   */
  public void validateCondition(
      int sortOrder, Long baseId, List<String> values, List<EventFormQuestion> siblings) {
    if (baseId == null) {
      return;
    }
    if (values == null || values.isEmpty()) {
      throw new BusinessException(CONDITION_VALUES_EMPTY);
    }
    for (String value : values) {
      if (value == null || value.isBlank()) {
        throw new BusinessException(CONDITION_VALUES_EMPTY);
      }
    }

    EventFormQuestion base =
        siblings.stream()
            .filter(q -> baseId.equals(q.getId()))
            .findFirst()
            .orElseThrow(() -> new BusinessException(CONDITION_QUESTION_NOT_FOUND));

    if (!base.getType().isUsableAsCondition()) {
      throw new BusinessException(CONDITION_QUESTION_TYPE_INVALID);
    }
    // 앞 순서만 참조한다. 이 한 줄이 순환 참조를 구조적으로 막는다.
    if (base.getSortOrder() >= sortOrder) {
      throw new BusinessException(CONDITION_QUESTION_NOT_BEFORE);
    }

    Set<String> allowed = allowedConditionValues(base);
    for (String value : values) {
      if (!allowed.contains(value)) {
        throw new BusinessException(CONDITION_VALUE_INVALID);
      }
    }
  }

  private Set<String> allowedConditionValues(EventFormQuestion base) {
    if (base.getType() == QuestionType.AGREEMENT) {
      return AGREEMENT_VALUES;
    }
    Set<String> values = new HashSet<>();
    if (base.getOptions() != null) {
      for (QuestionOption option : base.getOptions()) {
        if (option != null && option.value() != null) {
          values.add(option.value());
        }
      }
    }
    return values;
  }

  /** 이 질문을 기준으로 삼는 조건이 남아 있으면 지우거나 유형을 바꿀 수 없다. */
  public void validateNotReferenced(Long questionId, List<EventFormQuestion> siblings) {
    boolean referenced =
        siblings.stream()
            .anyMatch(q -> !q.getId().equals(questionId) && questionId.equals(q.getVisibleWhenQuestionId()));
    if (referenced) {
      throw new BusinessException(CONDITION_REFERENCED);
    }
  }

  /**
   * 다른 질문이 조건으로 삼는 선택지는 지울 수 없다.
   *
   * <p>지우고 나면 그 조건은 어떤 답으로도 참이 되지 않는 값을 가리킨다. 조건을 건 질문은 폼에서 조용히 사라지는데, 관리자 화면에는 그대로 남아 있어 왜 아무도 답하지
   * 않는지 알 길이 없다. 유형을 바꿀 때만 막고 선택지 하나를 지울 때는 놔두면 같은 구멍이 남는다.
   */
  public void validateOptionValuesNotReferenced(
      Long questionId,
      List<QuestionOption> before,
      List<QuestionOption> after,
      List<EventFormQuestion> siblings) {
    if (before == null || before.isEmpty()) {
      return;
    }
    Set<String> afterValues = new HashSet<>();
    if (after != null) {
      for (QuestionOption option : after) {
        if (option != null && option.value() != null) {
          afterValues.add(option.value());
        }
      }
    }
    for (QuestionOption option : before) {
      if (option == null || option.value() == null || afterValues.contains(option.value())) {
        continue;
      }
      for (EventFormQuestion sibling : siblings) {
        if (sibling.getId().equals(questionId)) {
          continue;
        }
        if (questionId.equals(sibling.getVisibleWhenQuestionId())
            && sibling.referencesOptionValue(option.value())) {
          throw new BusinessException(CONDITION_OPTION_REFERENCED);
        }
      }
    }
  }

  /** 새 순서에서도 모든 조건이 앞 질문을 가리키는지 본다. */
  public void validateOrderKeepsConditions(List<EventFormQuestion> inNewOrder) {
    for (int i = 0; i < inNewOrder.size(); i++) {
      EventFormQuestion question = inNewOrder.get(i);
      if (!question.hasCondition()) {
        continue;
      }
      int baseIndex = indexOf(inNewOrder, question.getVisibleWhenQuestionId());
      if (baseIndex < 0 || baseIndex >= i) {
        throw new BusinessException(CONDITION_ORDER_BROKEN);
      }
    }
  }

  private int indexOf(List<EventFormQuestion> questions, Long id) {
    for (int i = 0; i < questions.size(); i++) {
      if (questions.get(i).getId().equals(id)) {
        return i;
      }
    }
    return -1;
  }

  /** 신청이 있을 때는 선택지를 지울 수 없다. 라벨만 고칠 수 있다. */
  public void validateOptionValuesKept(List<QuestionOption> before, List<QuestionOption> after) {
    if (before == null || before.isEmpty()) {
      return;
    }
    Set<String> afterValues = new HashSet<>();
    if (after != null) {
      for (QuestionOption option : after) {
        if (option != null && option.value() != null) {
          afterValues.add(option.value());
        }
      }
    }
    for (QuestionOption option : before) {
      if (option != null && option.value() != null && !afterValues.contains(option.value())) {
        throw new BusinessException(OPTION_VALUE_LOCKED);
      }
    }
  }
}
