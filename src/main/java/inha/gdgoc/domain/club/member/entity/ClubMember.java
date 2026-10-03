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
import java.time.LocalDate;
import java.time.ZoneId;
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

  /** 참여 신청. 리더가 승인하기 전까지 PENDING 이다. */
  public static ClubMember apply(Club club, User user, String applyMessage, Instant now) {
    ClubMember member = new ClubMember();
    member.club = club;
    member.user = user;
    member.status = ClubMemberStatus.PENDING;
    member.applyMessage = applyMessage;
    member.appliedAt = now;
    return member;
  }

  /** 개설자. 신청 없이 바로 ACTIVE 로 들어간다. */
  public static ClubMember founder(Club club, User user, Instant now) {
    ClubMember member = apply(club, user, null, now);
    member.approve(now);
    return member;
  }

  public void approve(Instant now) {
    this.status = ClubMemberStatus.ACTIVE;
    this.joinedAt = now;
  }

  public void reject() {
    this.status = ClubMemberStatus.REJECTED;
  }

  public void cancel() {
    this.status = ClubMemberStatus.CANCELED;
  }

  public void leave(Instant now) {
    this.status = ClubMemberStatus.LEFT;
    this.leftAt = now;
  }

  public void kick(Instant now) {
    this.status = ClubMemberStatus.KICKED;
    this.leftAt = now;
  }

  /**
   * 그 날짜의 명단에 드는가.
   *
   * <p>그날 합류한 사람은 들고, 그날 나간 사람은 빠진다 — 경계는 다음 날 0시(zone 기준)다. 탈퇴·강퇴 뒤에도 행이 남으므로 지난 회차 명단이 바뀌지
   * 않는다.
   */
  public boolean wasActiveOn(LocalDate date, ZoneId zone) {
    if (joinedAt == null) {
      return false;
    }
    Instant nextDayStart = date.plusDays(1).atStartOfDay(zone).toInstant();
    return joinedAt.isBefore(nextDayStart) && (leftAt == null || !leftAt.isBefore(nextDayStart));
  }
}
