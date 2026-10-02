package inha.gdgoc.domain.club.leader.entity;

import inha.gdgoc.domain.club.club.enums.ClubCategory;
import inha.gdgoc.domain.club.leader.enums.ClubOpenRequestStatus;
import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부원의 소모임 개설 신청. 승인하면 이끔이 권한이 생긴다.
 */
@Entity
@Table(name = "club_open_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubOpenRequest extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false, length = 16)
  private ClubCategory category;

  @Column(name = "summary", nullable = false, length = 200)
  private String summary;

  @Column(name = "goal", columnDefinition = "TEXT")
  private String goal;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private ClubOpenRequestStatus status;

  @Column(name = "reject_reason", length = 500)
  private String rejectReason;

  @Column(name = "reviewed_by")
  private Long reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  public static ClubOpenRequest create(
      User user, String name, ClubCategory category, String summary, String goal) {
    ClubOpenRequest request = new ClubOpenRequest();
    request.user = user;
    request.name = name;
    request.category = category;
    request.summary = summary;
    request.goal = goal;
    request.status = ClubOpenRequestStatus.PENDING;
    return request;
  }

  public void approve(Long reviewerId, Instant now) {
    this.status = ClubOpenRequestStatus.APPROVED;
    this.reviewedBy = reviewerId;
    this.reviewedAt = now;
  }

  public void reject(Long reviewerId, String reason, Instant now) {
    this.status = ClubOpenRequestStatus.REJECTED;
    this.rejectReason = reason;
    this.reviewedBy = reviewerId;
    this.reviewedAt = now;
  }
}
