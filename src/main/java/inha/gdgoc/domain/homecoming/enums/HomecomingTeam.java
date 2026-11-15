package inha.gdgoc.domain.homecoming.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 운영진 OB 가 고른 활동 팀. 과거 팀명이 달랐어도 신청자가 역할 설명을 보고 직접 고른 값을 그대로 저장·출력한다 — 서버가 재분류하지 않는다.
 *
 * <p>계정의 {@code TeamType} 과 따로 둔 이유: 여기에는 Organizer 가 팀으로 있고, 명찰에 화면 표기('PR·Design')를 그대로 써야 한다.
 */
@Getter
public enum HomecomingTeam {
  ORGANIZER("Organizer"),
  HR("HR"),
  BD("BD"),
  TECH("Tech"),
  PR_DESIGN("PR·Design");

  private final String label;

  HomecomingTeam(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  @JsonCreator
  public static HomecomingTeam from(String raw) {
    if (raw == null) return null;
    String v = raw.trim();
    for (HomecomingTeam t : values()) {
      if (t.label.equalsIgnoreCase(v) || t.name().equalsIgnoreCase(v)) return t;
    }
    return null;
  }
}
