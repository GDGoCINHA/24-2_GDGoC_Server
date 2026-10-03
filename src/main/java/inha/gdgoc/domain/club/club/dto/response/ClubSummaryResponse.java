package inha.gdgoc.domain.club.club.dto.response;

import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.enums.ClubCategory;
import inha.gdgoc.domain.club.club.enums.ClubRecruitStatus;

/** 목록 카드. */
public record ClubSummaryResponse(
    Long id,
    String name,
    ClubCategory category,
    String summary,
    String imageUrl,
    String leaderName,
    long memberCount,
    Integer capacity,
    ClubRecruitStatus recruitStatus) {

  public static ClubSummaryResponse of(Club club, long memberCount) {
    return new ClubSummaryResponse(
        club.getId(),
        club.getName(),
        club.getCategory(),
        club.getSummary(),
        club.getImageUrl(),
        club.getLeader().getName(),
        memberCount,
        club.getCapacity(),
        club.getRecruitStatus());
  }
}
