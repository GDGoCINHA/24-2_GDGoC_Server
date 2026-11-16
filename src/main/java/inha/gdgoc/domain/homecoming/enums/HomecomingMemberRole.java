package inha.gdgoc.domain.homecoming.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** OB 의 활동 구분. 계정 역할({@code UserRole})과 무관하게 신청자가 스스로 고른다. */
@Getter
public enum HomecomingMemberRole {
  ORGANIZER("organizer", "운영진"),
  MEMBER("member", "일반 멤버");

  private final String value;
  private final String label;

  HomecomingMemberRole(String value, String label) {
    this.value = value;
    this.label = label;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static HomecomingMemberRole from(String raw) {
    if (raw == null) return null;
    for (HomecomingMemberRole r : values()) {
      if (r.value.equalsIgnoreCase(raw.trim()) || r.name().equalsIgnoreCase(raw.trim())) return r;
    }
    return null;
  }
}
