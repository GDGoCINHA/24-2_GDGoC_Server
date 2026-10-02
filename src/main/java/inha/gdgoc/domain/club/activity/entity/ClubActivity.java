package inha.gdgoc.domain.club.activity.entity;

import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import inha.gdgoc.domain.club.activity.exception.ClubActivityErrorCode;
import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.schedule.entity.ClubSchedule;
import inha.gdgoc.global.entity.BaseEntity;
import inha.gdgoc.global.exception.BusinessException;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 활동 기록 = 활동 인증.
 *
 * <p><b>인증 완료(APPROVED)되면 출석을 포함해 수정할 수 없다.</b>
 */
@Entity
@Table(name = "club_activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubActivity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "club_id", nullable = false)
  private Club club;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "schedule_id")
  private ClubSchedule schedule;

  @Column(name = "activity_date", nullable = false)
  private LocalDate activityDate;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "progress_note", columnDefinition = "TEXT")
  private String progressNote;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 24)
  private ClubActivityStatus status;

  @Column(name = "revision_reason", length = 1000)
  private String revisionReason;

  @Column(name = "reviewed_by")
  private Long reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Column(name = "submitted_at", nullable = false)
  private Instant submittedAt;

  @Column(name = "created_by")
  private Long createdBy;

  /** 리더가 제출한다. 제출하면 바로 확인 중(PENDING)이다. */
  public static ClubActivity create(
      Club club,
      ClubSchedule schedule,
      LocalDate activityDate,
      String content,
      String progressNote,
      Long creatorId,
      Instant now) {
    ClubActivity activity = new ClubActivity();
    activity.club = club;
    activity.schedule = schedule;
    activity.activityDate = activityDate;
    activity.content = content;
    activity.progressNote = progressNote;
    activity.createdBy = creatorId;
    activity.status = ClubActivityStatus.PENDING;
    activity.submittedAt = now;
    return activity;
  }

  /**
   * 확인 중·보완 요청 상태의 기록을 고친다. 고치면 다시 확인 중이 된다.
   *
   * <p>출석 명단은 여기서 다루지 않는다 — 활동일이 바뀌면 서비스가 명단을 다시 만든다.
   */
  public void update(
      ClubSchedule schedule,
      LocalDate activityDate,
      String content,
      String progressNote,
      Instant now) {
    requireEditable();
    this.schedule = schedule;
    this.activityDate = activityDate;
    this.content = content;
    this.progressNote = progressNote;
    this.status = ClubActivityStatus.PENDING;
    this.submittedAt = now;
  }

  /** 운영진의 인증 완료. 최종 상태라 이후로는 출석을 포함해 아무것도 바꿀 수 없다. */
  public void approve(Long reviewerId, Instant now) {
    requireEditable();
    this.status = ClubActivityStatus.APPROVED;
    this.reviewedBy = reviewerId;
    this.reviewedAt = now;
  }

  /**
   * 운영진의 보완 요청.
   *
   * <p>사유는 리더가 고쳐서 다시 낸 뒤에도 남겨 둔다 — 운영진이 재검토할 때 무엇을 요청했는지 봐야 한다. 다시 보완 요청하면 덮어쓴다.
   */
  public void requestRevision(Long reviewerId, String reason, Instant now) {
    requireEditable();
    this.status = ClubActivityStatus.REVISION_REQUESTED;
    this.revisionReason = reason;
    this.reviewedBy = reviewerId;
    this.reviewedAt = now;
  }

  public boolean isApproved() {
    return status == ClubActivityStatus.APPROVED;
  }

  /** 인증 완료된 기록이면 409. 출석·사진처럼 이 엔티티 밖의 것을 고치기 전에도 부른다. */
  public void requireEditable() {
    if (isApproved()) {
      throw new BusinessException(ClubActivityErrorCode.ACTIVITY_LOCKED);
    }
  }
}
