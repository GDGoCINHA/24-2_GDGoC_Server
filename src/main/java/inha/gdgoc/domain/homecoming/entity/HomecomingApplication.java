package inha.gdgoc.domain.homecoming.entity;

import inha.gdgoc.domain.homecoming.enums.AttendType;
import inha.gdgoc.domain.homecoming.enums.HomecomingMemberRole;
import inha.gdgoc.domain.homecoming.enums.HomecomingStatus;
import inha.gdgoc.domain.homecoming.enums.HomecomingTeam;
import inha.gdgoc.domain.homecoming.service.HomecomingProfile;
import inha.gdgoc.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 홈커밍데이 신청 한 건. 로그인 없이 받으므로 신원을 행에 직접 적는다.
 *
 * <p>전화번호 중복은 ACTIVE 끼리만 막는다(부분 유니크 인덱스, 마이그레이션 참고). 엔티티 어노테이션으로는 표현할 수 없어 테스트(H2)에는
 * 없고, 서비스의 사전 확인이 같은 규칙을 지킨다.
 */
@Entity
@Table(
    name = "homecoming_application",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_homecoming_application_request",
            columnNames = {"request_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HomecomingApplication extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "request_id", nullable = false, length = 64)
  private String requestId;

  @Enumerated(EnumType.STRING)
  @Column(name = "attend_type", nullable = false, length = 10)
  private AttendType attendType;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  @Column(name = "phone", nullable = false, length = 13)
  private String phone;

  @Column(name = "year_start")
  private Integer yearStart;

  @Column(name = "year_end")
  private Integer yearEnd;

  @Enumerated(EnumType.STRING)
  @Column(name = "member_role", length = 16)
  private HomecomingMemberRole memberRole;

  @Enumerated(EnumType.STRING)
  @Column(name = "team", length = 16)
  private HomecomingTeam team;

  @Column(name = "is_late", nullable = false)
  private boolean late;

  @Column(name = "arrival_time", length = 5)
  private String arrivalTime;

  @Column(name = "message", length = 1000)
  private String message;

  @Column(name = "privacy_agreed", nullable = false)
  private boolean privacyAgreed;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private HomecomingStatus status;

  @Column(name = "submitted_at", nullable = false)
  private Instant submittedAt;

  @Column(name = "canceled_at")
  private Instant canceledAt;

  @Column(name = "cancel_reason", length = 200)
  private String cancelReason;

  @Column(name = "last_modified_by")
  private Long lastModifiedBy;

  public static HomecomingApplication submit(
      String requestId, HomecomingProfile profile, Instant submittedAt) {
    HomecomingApplication a = new HomecomingApplication();
    a.requestId = requestId;
    a.apply(profile);
    a.privacyAgreed = true;
    a.status = HomecomingStatus.ACTIVE;
    a.submittedAt = submittedAt;
    return a;
  }

  /** 운영진이 신청자의 변경 요청을 반영한다. */
  public void edit(HomecomingProfile profile, Long adminId) {
    apply(profile);
    this.lastModifiedBy = adminId;
  }

  public void cancel(String reason, Long adminId, Instant now) {
    this.status = HomecomingStatus.CANCELED;
    this.cancelReason = reason;
    this.canceledAt = now;
    this.lastModifiedBy = adminId;
  }

  public boolean isActive() {
    return status == HomecomingStatus.ACTIVE;
  }

  private void apply(HomecomingProfile p) {
    this.attendType = p.attendType();
    this.name = p.name();
    this.phone = p.phone();
    this.yearStart = p.yearStart();
    this.yearEnd = p.yearEnd();
    this.memberRole = p.memberRole();
    this.team = p.team();
    this.late = p.late();
    this.arrivalTime = p.arrivalTime();
    this.message = p.message();
  }

  /** 명찰·관리 화면 표기. 예: {@code 2022 ~ 2024 · 운영진 · BD}. 불참이면 빈 문자열. */
  public String activitySummary() {
    if (yearStart == null || yearEnd == null || memberRole == null) return "";
    StringBuilder sb = new StringBuilder();
    sb.append(yearStart).append(" ~ ").append(yearEnd).append(" · ").append(memberRole.getLabel());
    if (team != null) sb.append(" · ").append(team.getLabel());
    return sb.toString();
  }
}
