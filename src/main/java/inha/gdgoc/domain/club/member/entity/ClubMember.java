package inha.gdgoc.domain.club.member.entity;

import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.member.enums.ClubMemberStatus;
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
 * 소모임 멤버십.
 *
 * <p>탈퇴·강퇴·거절해도 행을 지우지 않는다. 지난 회차 명단이 {@code joinedAt}/{@code leftAt} 으로 계산된다. 재신청은 새 행이다.
 */
@Entity
@Table(name = "club_member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubMember extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "club_id", nullable = false)
  private Club club;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private ClubMemberStatus status;

  @Column(name = "apply_message", length = 500)
  private String applyMessage;

  @Column(name = "applied_at", nullable = false)
  private Instant appliedAt;

  @Column(name = "joined_at")
  private Instant joinedAt;

  @Column(name = "left_at")
  private Instant leftAt;
}
