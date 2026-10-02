package inha.gdgoc.domain.club.completion.enums;

/** 완주 현황의 경고. 막지 않고 보여주기만 한다 — 판단은 운영진이 한다. Web `ClubWarning`과 같은 문자열. */
public enum ClubCompletionWarning {
  /** 소모임 활동 기간이 비어 있어 주차 계산을 하지 않았다. */
  PERIOD_NOT_SET,
  /** 지금 팀원이 4명 미만이다 (이끔이 포함). */
  MEMBERS_UNDER_4,
  /** 이미 지난 주 중 쉬는 주가 아닌데 인정 활동이 없는 주가 있다. */
  WEEK_MISSED,
  /** 목표가 등록되지 않았다. */
  GOAL_NOT_SET,
  /** 활동 기간 밖의 활동 기록이 있다. 계산에서 뺐다. */
  OUT_OF_PERIOD,
  /** 지금 팀원 수가 모집 정원을 넘었다. */
  OVER_CAPACITY
}
