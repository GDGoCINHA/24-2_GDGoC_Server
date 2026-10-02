package inha.gdgoc.domain.club.leader.entity;

import inha.gdgoc.domain.user.entity.User;
import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 이끔이 권한(소모임 개설 권한). {@code revokedAt} 이 null 이면 유효하다.
 */
@Entity
@Table(name = "club_leader_grant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubLeaderGrant extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "granted_by")
  private Long grantedBy;

  @Column(name = "granted_at", nullable = false)
  private Instant grantedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  public static ClubLeaderGrant grant(User user, Long grantedBy, Instant now) {
    ClubLeaderGrant grant = new ClubLeaderGrant();
    grant.user = user;
    grant.grantedBy = grantedBy;
    grant.grantedAt = now;
    return grant;
  }

  /** 회수해도 이미 연 소모임의 이끔이 지위는 그대로다. 새로 여는 것만 막힌다. */
  public void revoke(Instant now) {
    this.revokedAt = now;
  }
}
