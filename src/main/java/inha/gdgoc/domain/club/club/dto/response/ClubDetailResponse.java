package inha.gdgoc.domain.club.club.dto.response;

import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.club.enums.ClubCategory;
import inha.gdgoc.domain.club.club.enums.ClubRecruitStatus;
import inha.gdgoc.domain.club.club.enums.ClubStatus;
import java.time.LocalDate;

/**
 * 상세.
 *
 * <p>{@code kakaoLink} 는 ACTIVE 멤버와 운영진에게만 채운다. 나머지에게는 null 이다.
 */
public record ClubDetailResponse(
    Long id,
    String name,
    ClubCategory category,
    String summary,
    String description,
    String activityMethod,
    String imageUrl,
    Long leaderId,
    String leaderName,
    long memberCount,
    Integer capacity,
    LocalDate startDate,
    LocalDate endDate,
    ClubRecruitStatus recruitStatus,
    ClubStatus status,
    String rejectReason,
    Long termId,
    String termName,
    String kakaoLink,
    ClubMembershipResponse myMembership) {

  public static ClubDetailResponse of(
      Club club, long memberCount, boolean showKakaoLink, ClubMembershipResponse myMembership) {
    return new ClubDetailResponse(
        club.getId(),
        club.getName(),
        club.getCategory(),
        club.getSummary(),
        club.getDescription(),
        club.getActivityMethod(),
        club.getImageUrl(),
        club.getLeader().getId(),
        club.getLeader().getName(),
        memberCount,
        club.getCapacity(),
        club.getStartDate(),
        club.getEndDate(),
        club.getRecruitStatus(),
        club.getStatus(),
        club.getRejectReason(),
        club.getTerm().getId(),
        club.getTerm().getName(),
        showKakaoLink ? club.getKakaoLink() : null,
        myMembership);
  }
}
