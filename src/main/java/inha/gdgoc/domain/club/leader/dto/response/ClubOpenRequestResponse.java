package inha.gdgoc.domain.club.leader.dto.response;

import inha.gdgoc.domain.club.club.enums.ClubCategory;
import inha.gdgoc.domain.club.leader.entity.ClubOpenRequest;
import inha.gdgoc.domain.club.leader.enums.ClubOpenRequestStatus;
import java.time.Instant;

public record ClubOpenRequestResponse(
    Long id,
    Long userId,
    String userName,
    String name,
    ClubCategory category,
    String summary,
    String goal,
    ClubOpenRequestStatus status,
    String rejectReason,
    Instant createdAt) {

  public static ClubOpenRequestResponse of(ClubOpenRequest request) {
    return new ClubOpenRequestResponse(
        request.getId(),
        request.getUser().getId(),
        request.getUser().getName(),
        request.getName(),
        request.getCategory(),
        request.getSummary(),
        request.getGoal(),
        request.getStatus(),
        request.getRejectReason(),
        request.getCreatedAt());
  }
}
