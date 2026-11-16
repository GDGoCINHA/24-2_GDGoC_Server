package inha.gdgoc.domain.homecoming.entity;

import inha.gdgoc.domain.homecoming.enums.ConsentPurpose;
import inha.gdgoc.domain.homecoming.enums.ConsentState;
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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 목적별 동의 한 건. 거부도 {@code agreed=false} 로 남긴다 — 어떤 안내문을 보고 거부했는지도 기록이다.
 *
 * <p>철회는 행을 지우지 않고 {@code withdrawnAt} 을 채운다. 실제 개인정보 삭제는 기한 만료·철회 목록을 보고 담당자가 한다.
 */
@Entity
@Table(
    name = "homecoming_consent",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_homecoming_consent_purpose",
            columnNames = {"application_id", "purpose"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HomecomingConsent extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "application_id", nullable = false)
  private HomecomingApplication application;

  @Enumerated(EnumType.STRING)
  @Column(name = "purpose", nullable = false, length = 20)
  private ConsentPurpose purpose;

  @Column(name = "agreed", nullable = false)
  private boolean agreed;

  @Column(name = "notice_version", nullable = false, length = 40)
  private String noticeVersion;

  @Column(name = "decided_at", nullable = false)
  private Instant decidedAt;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "withdrawn_at")
  private Instant withdrawnAt;

  @Column(name = "withdrawn_by")
  private Long withdrawnBy;

  public static HomecomingConsent record(
      HomecomingApplication application,
      ConsentPurpose purpose,
      boolean agreed,
      String noticeVersion,
      Instant decidedAt,
      Instant expiresAt) {
    HomecomingConsent c = new HomecomingConsent();
    c.application = application;
    c.purpose = purpose;
    c.agreed = agreed;
    c.noticeVersion = noticeVersion;
    c.decidedAt = decidedAt;
    c.expiresAt = agreed ? expiresAt : null;
    return c;
  }

  public void withdraw(Long adminId, Instant now) {
    this.withdrawnAt = now;
    this.withdrawnBy = adminId;
  }

  public ConsentState stateAt(Instant now) {
    if (!agreed) return ConsentState.REFUSED;
    if (withdrawnAt != null) return ConsentState.WITHDRAWN;
    if (expiresAt != null && !now.isBefore(expiresAt)) return ConsentState.EXPIRED;
    return ConsentState.ACTIVE;
  }
}
