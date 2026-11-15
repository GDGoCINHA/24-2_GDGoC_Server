package inha.gdgoc.domain.homecoming.enums;

import lombok.Getter;

@Getter
public enum ConsentPurpose {
  /** 이번 행사 운영(참석 확인·안내 연락·명찰 제작). 필수. */
  PRIVACY("행사 운영", false),
  /** 이후 동아리 행사 초청. 선택. */
  EVENT_INVITE("이후 행사 초청", true),
  /** OB 인재풀(연사·멘토·심사위원·프로젝트 협력 제안). 선택. */
  TALENT_POOL("OB 인재풀", true);

  private final String label;

  /** 장기 등록 목적 — 기한(2027-12-31)이 붙고 목록·철회 대상이 된다. */
  private final boolean longTerm;

  ConsentPurpose(String label, boolean longTerm) {
    this.label = label;
    this.longTerm = longTerm;
  }
}
