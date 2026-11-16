package inha.gdgoc.domain.homecoming.dto.response;

import inha.gdgoc.domain.homecoming.entity.HomecomingApplication;
import inha.gdgoc.domain.homecoming.enums.AttendType;

/** 신청자에게는 번호와 유형만 돌려준다. 완료 화면 문구가 유형으로 갈린다. */
public record HomecomingApplyResponse(Long id, AttendType attendType) {

  public static HomecomingApplyResponse from(HomecomingApplication a) {
    return new HomecomingApplyResponse(a.getId(), a.getAttendType());
  }
}
