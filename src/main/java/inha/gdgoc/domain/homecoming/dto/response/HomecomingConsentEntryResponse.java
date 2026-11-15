package inha.gdgoc.domain.homecoming.dto.response;

import inha.gdgoc.domain.homecoming.entity.HomecomingApplication;
import inha.gdgoc.domain.homecoming.entity.HomecomingConsent;
import inha.gdgoc.domain.homecoming.enums.ConsentPurpose;
import inha.gdgoc.domain.homecoming.enums.ConsentState;
import inha.gdgoc.domain.homecoming.enums.HomecomingMemberRole;
import java.time.Instant;

/**
 * 장기 등록 목록 한 줄. 장기 이용 범위(이름·전화번호·활동 기간·활동 구분)만 담는다 — 팀·늦참·요청사항은 여기 넣지 않는다.
 */
public record HomecomingConsentEntryResponse(
    Long applicationId,
    String name,
    String phone,
    Integer yearStart,
    Integer yearEnd,
    HomecomingMemberRole memberRole,
    ConsentPurpose purpose,
    ConsentState state,
    String noticeVersion,
    Instant decidedAt,
    Instant expiresAt,
    Instant withdrawnAt) {

  public static HomecomingConsentEntryResponse of(HomecomingConsent c, Instant now) {
    HomecomingApplication a = c.getApplication();
    return new HomecomingConsentEntryResponse(
        a.getId(),
        a.getName(),
        a.getPhone(),
        a.getYearStart(),
        a.getYearEnd(),
        a.getMemberRole(),
        c.getPurpose(),
        c.stateAt(now),
        c.getNoticeVersion(),
        c.getDecidedAt(),
        c.getExpiresAt(),
        c.getWithdrawnAt());
  }
}
