package inha.gdgoc.domain.club.club.dto.response;

import inha.gdgoc.domain.club.member.enums.ClubMemberStatus;

/** 내 소모임 탭. */
public record MyClubResponse(ClubSummaryResponse club, ClubMemberStatus status, boolean isLeader) {}
