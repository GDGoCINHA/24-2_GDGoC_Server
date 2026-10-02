package inha.gdgoc.domain.club.club.dto.response;

import inha.gdgoc.domain.club.member.enums.ClubMemberStatus;

/** 나와 이 소모임의 관계. 신청한 적 없으면 상세 응답에서 null 이다. */
public record ClubMembershipResponse(ClubMemberStatus status, boolean isLeader) {}
