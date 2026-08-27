package inha.gdgoc.domain.eventapplication.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

/**
 * 신청 폼의 질문 유형.
 *
 * <p>관리자는 질문을 몇 개든 추가할 수 있지만 유형은 이 목록에서 고른다. 유형마다 화면에 그리는 방법도, 검증 규칙도, 저장 형태도 다르기 때문이다. 새 유형을 늘리려면 여기와
 * 프론트 렌더러를 함께 고쳐야 한다.
 */
@Getter
public enum QuestionType {
  SHORT_TEXT(false, false),
  LONG_TEXT(false, false),
  NUMBER(false, false),
  DATE(false, false),
  SINGLE_CHOICE(true, true),
  MULTI_CHOICE(true, true),
  DROPDOWN(true, true),
  FILE(false, false),
  AGREEMENT(false, true);

  /** 선택지 목록이 반드시 있어야 하는 유형인지. */
  private final boolean requiresOptions;

  /** 다른 질문의 표시 조건에서 기준으로 삼을 수 있는 유형인지. */
  private final boolean usableAsCondition;

  QuestionType(boolean requiresOptions, boolean usableAsCondition) {
    this.requiresOptions = requiresOptions;
    this.usableAsCondition = usableAsCondition;
  }

  /**
   * 요청 본문의 문자열을 유형으로 바꾼다.
   *
   * <p>null 을 먼저 거른다. {@code Set.of(...).contains(null)} 은 NPE 를 던져 400 이어야 할 응답이 500 이 된다.
   */
  @JsonCreator
  public static QuestionType from(String raw) {
    if (raw == null) {
      return null;
    }
    String normalized = raw.trim().replace('-', '_').replace(' ', '_').toUpperCase();
    if (normalized.isBlank()) {
      return null;
    }
    for (QuestionType type : values()) {
      if (type.name().equals(normalized)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown question type: " + raw);
  }
}
