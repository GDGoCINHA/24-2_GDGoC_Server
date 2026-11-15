package inha.gdgoc.domain.homecoming.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 참석 유형. 유형마다 신청 화면이 거치는 단계가 다르다 — 활동 정보는 불참(NO)만 받지 않고, 늦참·요청사항은 본 행사(MAIN·BOTH)만 받는다.
 *
 * <p>JSON 은 화면이 쓰는 소문자 값({@code main})으로 주고받는다. 모르는 값은 null 로 읽혀 {@code @NotNull} 검증에서 400 이 된다.
 */
@Getter
public enum AttendType {
  MAIN("main", "본 행사만", "참석"),
  AFTER("after", "뒤풀이만", "참석"),
  BOTH("both", "본 행사+뒤풀이", "참석"),
  MAYBE("maybe", "아직 미정", "미정"),
  NO("no", "불참", "불참");

  private final String value;
  private final String label;

  /** HR 이 Notion 'OB 연락 명단'의 '2026 참석' 칸에 옮겨 적는 값. */
  private final String notionStatus;

  AttendType(String value, String label, String notionStatus) {
    this.value = value;
    this.label = label;
    this.notionStatus = notionStatus;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static AttendType from(String raw) {
    if (raw == null) return null;
    for (AttendType t : values()) {
      if (t.value.equalsIgnoreCase(raw.trim()) || t.name().equalsIgnoreCase(raw.trim())) return t;
    }
    return null;
  }

  public boolean hasActivity() {
    return this != NO;
  }

  /** 늦참·요청사항 단계를 거치는 유형. */
  public boolean hasExtra() {
    return this == MAIN || this == BOTH;
  }

  /** 명찰을 만드는 대상 — 본 행사에 오는 사람. */
  public boolean attendsMain() {
    return this == MAIN || this == BOTH;
  }

  public boolean attendsAfterParty() {
    return this == AFTER || this == BOTH;
  }
}
