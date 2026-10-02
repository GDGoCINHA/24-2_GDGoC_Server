package inha.gdgoc.domain.club.activity.entity;

import inha.gdgoc.domain.club.activity.enums.ClubActivityStatus;
import inha.gdgoc.domain.club.club.entity.Club;
import inha.gdgoc.domain.club.schedule.entity.ClubSchedule;
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
}
