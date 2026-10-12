package inha.gdgoc.domain.club.member.dto.response;

import inha.gdgoc.domain.club.member.enums.ClubMemberStatus;
import inha.gdgoc.domain.user.entity.User;

/**
 * 멤버 추가 화면의 검색 결과 한 줄. 일반 부원인 리더도 보므로 연락처는 주지 않는다.
 *
 * <p>학번은 앞 4자리(입학 연도)만 준다 — 동명이인을 가를 만큼만. {@code memberStatus} 는 이 소모임에서 신청 중(PENDING)이거나
 * 멤버(ACTIVE)면 그 값, 아니면 null 이다.
 */
public record ClubMemberCandidateResponse(
    Long userId, String name, String major, String studentId, ClubMemberStatus memberStatus) {

  private static final int VISIBLE_DIGITS = 4;

  public static ClubMemberCandidateResponse of(User user, ClubMemberStatus memberStatus) {
    return new ClubMemberCandidateResponse(
        user.getId(), user.getName(), user.getMajor(), mask(user.getStudentId()), memberStatus);
  }

  static String mask(String studentId) {
    if (studentId == null || studentId.length() <= VISIBLE_DIGITS) {
      return null;
    }
    return studentId.substring(0, VISIBLE_DIGITS)
        + "*".repeat(studentId.length() - VISIBLE_DIGITS);
  }
}
