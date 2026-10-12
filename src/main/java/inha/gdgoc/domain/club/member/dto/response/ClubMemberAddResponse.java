package inha.gdgoc.domain.club.member.dto.response;

/** 일괄 추가 결과. {@code approved} 는 신청 중이던 사람, {@code skipped} 는 이미 멤버였던 사람이다. */
public record ClubMemberAddResponse(int added, int approved, int skipped) {}
