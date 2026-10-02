package inha.gdgoc.domain.club.member.dto.response;

import inha.gdgoc.domain.club.member.entity.ClubMember;
import inha.gdgoc.domain.club.member.enums.ClubMemberStatus;
import java.time.Instant;

/** 멤버·신청자 한 줄. {@code memberId} 는 club_member 행 id 다(승인·강퇴할 때 쓴다). */
public record ClubMemberResponse(
    Long memberId,
    Long userId,
    String name,
    String major,
    ClubMemberStatus status,
    boolean isLeader,
    String applyMessage,
    Instant appliedAt,
    Instant joinedAt) {

  public static ClubMemberResponse of(ClubMember member, Long leaderId) {
    return new ClubMemberResponse(
        member.getId(),
        member.getUser().getId(),
        member.getUser().getName(),
        member.getUser().getMajor(),
        member.getStatus(),
        member.getUser().getId().equals(leaderId),
        member.getApplyMessage(),
        member.getAppliedAt(),
        member.getJoinedAt());
  }
}
